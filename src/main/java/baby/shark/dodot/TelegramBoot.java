package baby.shark.dodot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class TelegramBoot extends TelegramLongPollingBot {

    @Value("${bot.bot-name}")
    private String botName;
    @Value("${bot.bot-key}")
    private String botKey;
    private final Map<String, List<String>> map = new HashMap<>();
    private final Queue<Data> queue = new LinkedList<>();

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botKey;
    }

    class Data {
        LocalDateTime time;
        String message;
        String userName;

    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().isGroupMessage()) {
                groupMessage(update);
            } else {
                directMessage(update);
            }

        }
    }

    private void directMessage(Update update) {
        String text = update.getMessage().getText();

        String content;
        if ("/start".equalsIgnoreCase(text)) {
            content = "Add this message to your group and then this bot will read conversations and remembers key-value messages, the next time it sees the same message, it will reply with the saved response." +
                    "to add custom phrase type: ADD key:can i@value:no!";
        } else if (text.startsWith("ADD")) {

            int i = text.indexOf("@");
            String key = text.substring(0, i);
            String value = text.substring(i);

            key = key.substring(key.indexOf(":") + 1);
            value = value.substring(value.indexOf(":") + 1);
            List<String> list = map.getOrDefault(key, new ArrayList<>());
            if (list.contains(value)) {
                content = String.format("key: %s@value: %s already exist", key, value);
            } else {
                list.add(value.toLowerCase().trim());
                map.put(key.toLowerCase().trim(), list);
                content = String.format("key: %s@value: %s saved", key, value);
            }
        } else {
            content = "looks like i cannot help you with this request";
        }
        sendMessage(content, update.getMessage().getChatId());
    }

    private void groupMessage(Update update) {
        String userName = update.getMessage().getFrom().getUserName();
//            Document document = update.getMessage().getDocument();
        String text = update.getMessage().getText();

        if (map.containsKey(text.toLowerCase().trim())) {
            List<String> answers = map.get(text.toLowerCase().trim());
            Random rand = new Random();
            String answer = answers.get(rand.nextInt(answers.size()));
            sendMessage(answer, update.getMessage().getChatId());
        }

        Data currentData = new Data();
        currentData.message = text;
        currentData.time = LocalDateTime.now();
        currentData.userName = userName;
        queue.add(currentData);


        if (queue.size() == 2) {
            Data data = queue.poll();
            if (ChronoUnit.MINUTES.between(LocalDateTime.now(), data.time) < 1) {
                String key = data.message.toLowerCase().trim();
                String value = queue.poll().message.toLowerCase().trim();
                List<String> list = map.getOrDefault(key, new ArrayList<>());
                if (list.contains(value)) {
                    return;
                }
                list.add(value);
                map.put(key, list);
            }
        }
    }

    private void sendMessage(String message, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("ROBOT: " + message);
        try {
            execute(sendMessage);
        } catch (Exception exception) {
            System.out.println(exception);
        }
    }
}