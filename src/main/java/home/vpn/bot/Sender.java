package home.vpn.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;

public class Sender extends TelegramLongPollingBot {

    private Worker worker;
    @Override
    public String getBotUsername() {
        return Environment.bot_name;
    }

    @Override
    public String getBotToken() {
        return Environment.bot_token;
    }
    public void setWorker(Worker worker) {
        this.worker = worker;
        worker.setSender(this);
    }
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            worker.parse(chatId, messageText);
        }
    }
    public void sendMessage(Long chatId, String textToSend){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            Tools.logMessage("Error: " + e);
        }
    }

    public void sendFile(Long chatId, File file) {
        SendDocument sendDocumentRequest = new SendDocument();
        sendDocumentRequest.setChatId(chatId);
        sendDocumentRequest.setDocument(new InputFile(file));
        try {
            execute(sendDocumentRequest);
        } catch (TelegramApiException e) {
            Tools.logMessage("Error: " + e);
        }
    }

}
