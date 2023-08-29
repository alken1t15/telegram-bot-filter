package kz.alken1t15.filtertelegrambot.service;


import com.vdurmont.emoji.EmojiParser;
import kz.alken1t15.filtertelegrambot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.PromoteChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.adminrights.ChatAdministratorRights;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final List<String> banWords = new ArrayList<>();

    @Autowired
//    private UserService userService;
    final BotConfig config;

    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu of the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";

    public TelegramBot(BotConfig config) {
        this.config = config;
        banWords.add("Ужасный");
        banWords.add("плохой");
        banWords.add("Не благодарный");
        banWords.add("Плохо ведешь себя");
        banWords.add("Позер");
        List<BotCommand> listofCommands = new ArrayList();
        listofCommands.add(new BotCommand("/start", "get a welcome message"));
        listofCommands.add(new BotCommand("/mydata", "get your data stored"));
        listofCommands.add(new BotCommand("/deletedata", "delete my data"));
        listofCommands.add(new BotCommand("/help", "info how to use this bot"));
        listofCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();
            int messageIdToDelete = update.getMessage().getMessageId();
            switch (messageText) {
                case "/start":
//                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;

                case "/register":
                    register(chatId);
                    break;
                default:
                    for(String word : banWords){
                        if (messageText.contains(word)){
                            System.out.println("true");
                            BanChatMember banChatMember = new BanChatMember();
                            banChatMember.setChatId(chatId);
                            banChatMember.setUserId(userId);
                            banChatMember.setUntilDate(365);
                            DeleteMessage deleteMessage = new DeleteMessage();
                            deleteMessage.setChatId(chatId);
                            deleteMessage.setMessageId(messageIdToDelete);
                            try {
                                execute(deleteMessage);
                                execute(banChatMember);
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
//                    sendMessage(chatId, "Sorry, command was not recognized");

            }
        }
        else if(update.hasCallbackQuery()){
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.equals("YES_BUTTON")){
                String text = "You pressed Yes button";
                EditMessageText message = new EditMessageText();
                message.setChatId(chatId);
                message.setText(text);
                message.setMessageId((int) messageId);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            }
            else if(callbackData.equals("NO_BUTTON")){
                String text = "You pressed No button";
                EditMessageText message = new EditMessageText();
                message.setChatId(chatId);
                message.setText(text);
                message.setMessageId((int) messageId);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("Error occurred: " + e.getMessage());
                }
            }
        }
    }

    private void register(long chatId){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData("YES_BUTTON");

        var  noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("NO_BUTTON");

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

//    private void registerUser(Message msg) {
//        if (userService.findByChatId(msg.getChatId()).isEmpty()) {
//
//            var chatId = msg.getChatId();
//            var chat = msg.getChat();
//
//            User user = new User();
//
//            user.setChatId(chatId);
//            user.setFirstName(chat.getFirstName());
//            user.setLastName(chat.getLastName());
//            user.setUserName(chat.getUserName());
//            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
//
//            userService.saveReference(user);
//            log.info("user saved: " + user);
//        }
//    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");

        sendMessage(chatId, answer);
        log.info("Replied to user " + name);
    }

    private void sendMessage(long chatId, String textToSent) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSent);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("weatger");
        row.add("get random joke");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("register");
        row.add("check my data");
        row.add("delete my data");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
}