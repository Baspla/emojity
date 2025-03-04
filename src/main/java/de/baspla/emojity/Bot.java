package de.baspla.emojity;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;
import de.baspla.emojity.db.PlayerInformation;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.inlinequery.ChosenInlineQuery;
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Bot extends TelegramLongPollingBot {

    private static final int MAX_LOBBYS = 20;
    private static char[] additionalChars = {13, 65024, 65025, 65026, 65027, 65028, 65029, 65030, 65031, 65032, 65033,
            65034, 65035, 65036, 65037, 65038, 65039, 9792, 9793, 8205};
    private String BOTNAME;
    private String BOTTOKEN;
    private ArrayList<Lobby> lobbys;
    private SessionFactory sessionFactory;
    public boolean testmode = false;

    public Bot(String name, String token) {
        this.BOTNAME = name;
        this.BOTTOKEN = token;
        sessionFactory = new Configuration().configure().buildSessionFactory();
        lobbys = new ArrayList<Lobby>();
    }

    @Override
    public String getBotUsername() {
        return BOTNAME;
    }

    @Override
    public String getBotToken() {
        return BOTTOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                if (message.isCommand()) {
                    command(message);
                } else {
                    textmessage(message);
                }
            }
        } else if (update.hasInlineQuery()) {
            inlinequery(update.getInlineQuery());
        } else if (update.hasChosenInlineQuery()) {
            choseninlinequery(update.getChosenInlineQuery());
        } else if (update.hasCallbackQuery()) {
            callbackquery(update.getCallbackQuery());
        }
    }

    private boolean isAdmin(Long chatId) {
        PlayerInformation pi = loadPlayerInformation(chatId);
        return pi.isAdmin();
    }

    private void callbackquery(CallbackQuery callbackQuery) {
        boolean kill = false;
        String data = callbackQuery.getData();
        AnswerCallbackQuery answer = new AnswerCallbackQuery().setCallbackQueryId(callbackQuery.getId());
        if (data.startsWith("admin_") && isAdmin(callbackQuery.getMessage().getChatId())) {
            String cmd = data.substring(6).toLowerCase();
            switch (cmd) {
                case "stop":
                    for (int i = 0; i < lobbys.size(); i++) {
                        lobbys.get(i).stopGame();
                    }
                    sessionFactory.close();
                    answer.setText("Gute Nacht");
                    kill = true;
                    break;
                case "closeall":
                    int c = -1;
                    for (int i = 0; i < lobbys.size(); i++) {
                        lobbys.get(i).stopGame();
                        c = i;
                    }
                    answer.setText((c + 1) + " Lobbys wurden geschlossen");

                    break;
                case "testmode":
                    testmode = !testmode;
                    answer.setText("TestMode wurde " + (testmode ? "aktivert" : "deaktiviert"));
                    break;
                default:
                    answer.setText("Du hast Mist gebaut...");
                    answer.setShowAlert(true);
                    break;
            }
        }

        try {
            answerCallbackQuery(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        if (kill) {
            System.exit(0);
        }
    }

    private void choseninlinequery(ChosenInlineQuery chosenInlineQuery) {
        // TODO Auto-generated method stub
    }

    private void inlinequery(InlineQuery inlineQuery) {
        // TODO Auto-generated method stub
    }

    private void textmessage(Message message) {
        Lobby l = getLobbyByUser(message.getChatId());
        if (l != null) {
            l.textmessage(message);
            return;
        }
        send(message.getChatId(), "Du bist in keiner Lobby.");
    }

    private void command(Message message) {
        String[] parts = message.getText().split(" ");
        String command = parts[0].substring(1);
        String[] args = new String[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            args[i - 1] = parts[i];
        }
        System.out.println(message.getChatId() + " - " + message.getText());
        switch (command.toLowerCase()) {
            case "start":
                cmd_start(message, args);
                break;
            case "help":
                cmd_help(message, args);
                break;
            case "hilfe":
                cmd_help(message, args);
                break;
            case "lobbys":
                cmd_lobbys(message, args);
                break;
            case "join":
                cmd_join(message, args);
                break;
            case "play":
                cmd_play(message, args);
                break;
            case "leave":
                cmd_leave(message, args);
                break;
            case "stats":
                cmd_stats(message, args);
                break;
            case "points":
                cmd_points(message, args);
                break;
            case "punkte":
                cmd_points(message, args);
                break;
            case "mute":
                cmd_mute(message, args);
                break;
            case "stumm":
                cmd_mute(message, args);
                break;
            case "admin":
                cmd_admin(message, args);
                break;
            case "impressum":
                cmd_impressum(message, args);
                break;
            case "rechtliches":
                cmd_rechtliches(message, args);
                break;
            case "word":
                cmd_word(message, args);
                break;
            default:
                cmd_help(message, args);
                break;
        }
    }

    private void cmd_start(Message message, String[] args) {
        send(message.getChatId(),
                "Herzlich willkommen!\nWenn du direkt anfangen willst: /play\nUnd für eine Liste von Befehlen: /help");
    }

    private void cmd_help(Message message, String[] args) {
        String txt = "Hier ist eine Liste mit allen Befehlen:\n" + //
                "/help - Zeigt diesen Hilfetext\n" + //
                "/lobbys - Listet alle Lobbys auf\n" + //
                "/join <b>Lobby</b> <i>(Name)</i> - Tritt einer bestimmten Lobby bei\n" + //
                "/play <i>(Name)</i> - Tritt einer zufälligen Lobby bei\n" + //
                "/leave - Verlässt die aktuelle Lobby\n" + //
                "/stats - Zeigt deine Statistiken an\n" + //
                "/points - Zeigt die aktuellen Punkte in deiner Lobby an\n" + //
                "/impressum - Zeigt das Impressum an\n" + //
                "/mute - Schaltet Benachrichtigungen an und aus\n";
        send(message.getChatId(), txt);
    }

    private void cmd_lobbys(Message message, String[] args) {
        // TODO Inline Buttons
        String text;
        if (lobbys.size() == 0) {
            text = "Es sind keine aktiven Lobbys vorhanden.";
        } else {
            text = "Hier ist eine Liste aller aktiven Lobbys:\n";
            for (int i = 0; i < lobbys.size(); i++) {
                Lobby l = lobbys.get(i);
                text = text + "Lobby " + l.getLobbyid() + " - " + l.getStatusText() + " - " + l.getPlayercount() + "/"
                        + l.getMaxPlayercount() + " Spieler" + "\n";
            }
        }
        send(message.getChatId(), text);
    }

    private void cmd_join(Message message, String[] args) {
        if (getLobbyByUser(message.getChatId()) != null) {
            send(message.getChatId(), "Du bist schon in einer Lobby.");
            return;
        }
        if (args.length == 0) {
            send(message.getChatId(), "Bitte gib eine Lobby an.");
        } else if (args.length == 1) {
            String[] args2 = new String[args.length + 1];
            Collection<Emoji> emojis = EmojiManager.getAll();
            args2[args2.length - 1] = ((Emoji) emojis.toArray()[new Random().nextInt(emojis.size())]).getUnicode();
            send(message.getChatId(), "Du bist jetzt " + args2[args2.length - 1]);
            for (int i = 0; i < args.length; i++) {
                args2[i] = args[i];
                args = args2;
            }
        } else {
            String username = args[args.length - 1];
            if (!hasOnlyEmoji(username)) {
                send(message.getChatId(), "Bitte nutze nur Emojis in deinem Namen.");
                return;
            }
            String txt = "";
            for (int i = 0; i < args.length - 1; i++) {
                txt = txt + args[i];
            }
            txt = txt.toLowerCase();
            txt = txt.replaceAll("[^0-9]", "");
            try {
                int lobbyid = new Integer(txt);
                Lobby l = getLobbyById(lobbyid);
                if (l != null) {
                    l.join(message.getChatId(), username);
                } else {
                    createLobby(message.getChatId(), lobbyid, username);
                }
            } catch (NumberFormatException e) {
                send(message.getChatId(), "Diese Lobby konnte nicht gefunden werden.");
            }
        }
    }

    private void cmd_play(Message message, String[] args) {
        if (getLobbyByUser(message.getChatId()) != null) {
            send(message.getChatId(), "Du bist schon in einer Lobby.");
            return;
        }
        if (args.length == 0) {
            args = new String[1];
            Collection<Emoji> emojis = EmojiManager.getAll();
            args[0] = ((Emoji) emojis.toArray()[new Random().nextInt(emojis.size())]).getUnicode();
            send(message.getChatId(), "Du bist jetzt " + args[0]);
        }
        String username = args[0];
        if (!hasOnlyEmoji(username)) {
            send(message.getChatId(), "Bitte nutze nur Emojis in deinem Namen.");
            return;
        }
        float min = 1;
        Lobby minlobby = null;
        for (int i = 0; i < lobbys.size(); i++) {
            float perc = (float) lobbys.get(i).getPlayercount() / (float) lobbys.get(i).getMaxPlayercount();
            if (perc < min && !lobbys.get(i).isFull()) {
                min = perc;
                minlobby = lobbys.get(i);
            }
        }
        if (minlobby == null) {
            int i = 1;
            while (!createLobby(message.getChatId(), i, username)) {
                i++;
            }
        } else {
            minlobby.join(message.getChatId(), username);
        }

    }

    private void cmd_leave(Message message, String[] args) {
        Lobby l = getLobbyByUser(message.getChatId());
        if (l != null) {
            l.leave(message.getChatId());
        } else {
            send(message.getChatId(), "Du bist in keiner Lobby.");
        }
    }

    private void cmd_stats(Message message, String[] args) {
        PlayerInformation pinfo = loadPlayerInformation(message.getChatId());
        send(message.getChatId(),
                "Du hast bisher " + pinfo.getPoints() + " Sieg" + (pinfo.getPoints() == 1 ? "." : "e."));
    }

    private void cmd_points(Message message, String[] args) {
        Lobby l = getLobbyByUser(message.getChatId());
        if (l != null) {
            l.showPoints(message.getChatId());
        } else {
            send(message.getChatId(), "Du bist in keiner Lobby.");
        }
    }

    private void cmd_admin(Message message, String[] args) {
        if (isAdmin(message.getChatId())) {
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keys = new ArrayList<>();
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            row1.add(new InlineKeyboardButton("Stop").setCallbackData("admin_stop"));
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            row2.add(new InlineKeyboardButton("Alle Lobbys schließen").setCallbackData("admin_closeall"));
            List<InlineKeyboardButton> row3 = new ArrayList<>();
            row2.add(new InlineKeyboardButton("TestMode umstellen").setCallbackData("admin_testmode"));
            keys.add(row1);
            keys.add(row2);
            keys.add(row3);
            keyboard.setKeyboard(keys);
            sendMarkup(message.getChatId(), "Wähle eine Aktion aus:", keyboard);
        } else {
            if (args.length >= 1) {
                if (args[0].equals(BOTTOKEN)) {
                    PlayerInformation pi = loadPlayerInformation(message.getChatId());
                    pi.setAdmin(true);
                    update(pi);
                    send(message.getChatId(), "Du bist Admin!");
                }
            }
            cmd_help(message, args);
        }
    }

    private void cmd_mute(Message message, String[] args) {
        PlayerInformation pinfo = loadPlayerInformation(message.getChatId());
        boolean mute = pinfo.isMute();
        if (mute) {
            send(message.getChatId(), "Du erhältst wieder Benachrichtigungen.");
        } else {
            send(message.getChatId(), "Du erhältst keine Benachrichtigungen mehr.");
        }
        pinfo.setMute(!mute);
        update(pinfo);
    }

    private void cmd_impressum(Message message, String[] args) {
        send(message.getChatId(),
                "Dieser Bot wird von @TimMorgner entwickelt und verwaltet. Die Wörter werden aus dem Internet gesammelt und ich kann nicht alle auf Erklärbarkeit oder politische Korrektheit prüfen.\nBitte benehmt euch spielt fair.\nThis bot was made by @TimMorgner.\nPlay fair and keep it cool.\n/rechtliches");
    }

    private void cmd_rechtliches(Message message, String[] args) {

        try {
            File f = new File("disclaimer.txt");
            if(!f.exists()){
                f.createNewFile();
                String t = "<b>Haftung fuer Inhalte</b>\n" +
                        "Als Diensteanbieter sind wir gemaess Paragraph 7 Abs.1 TMG fuer eigene Inhalte auf diesem Bot nach den allgemeinen Gesetzen verantwortlich. Nach §§ 8 bis 10 TMG sind wir als Diensteanbieter jedoch nicht verpflichtet, uebermittelte oder gespeicherte fremde Informationen zu ueberwachen oder nach Umstaenden zu forschen, die auf eine rechtswidrige Taetigkeit hinweisen.\n" +
                        "Verpflichtungen zur Entfernung oder Sperrung der Nutzung von Informationen nach den allgemeinen Gesetzen bleiben hiervon unberuehrt. Eine diesbezuegliche Haftung ist jedoch erst ab dem Zeitpunkt der Kenntnis einer konkreten Rechtsverletzung moeglich. Bei Bekanntwerden von entsprechenden Rechtsverletzungen werden wir diese Inhalte umgehend entfernen.\n" +
                        "\n" +
                        "<b>Haftung fuer Links</b>\n" +
                        "Unser Angebot enthaelt Links zu externen Webseiten Dritter, auf deren Inhalte wir keinen Einfluss haben. Deshalb koennen wir fuer diese fremden Inhalte auch keine Gewaehr uebernehmen. Fuer die Inhalte der verlinkten Seiten ist stets der jeweilige Anbieter oder Betreiber der Seiten verantwortlich. Die verlinkten Seiten wurden zum Zeitpunkt der Verlinkung auf moegliche Rechtsverstoesse ueberprueft. Rechtswidrige Inhalte waren zum Zeitpunkt der Verlinkung nicht erkennbar.\n" +
                        "Eine permanente inhaltliche Kontrolle der verlinkten Seiten ist jedoch ohne konkrete Anhaltspunkte einer Rechtsverletzung nicht zumutbar. Bei Bekanntwerden von Rechtsverletzungen werden wir derartige Links umgehend entfernen.\n" +
                        "\n" +
                        "<b>Urheberrecht</b>\n" +
                        "Die durch die Botbetreiber erstellten Inhalte und Werke auf diesem Bot unterliegen dem deutschen Urheberrecht. Die Vervielfaeltigung, Bearbeitung, Verbreitung und jede Art der Verwertung ausserhalb der Grenzen des Urheberrechtes beduerfen der schriftlichen Zustimmung des jeweiligen Autors bzw. Erstellers. Downloads und Kopien dieses Bots sind nur fuer den privaten, nicht kommerziellen Gebrauch gestattet.\n" +
                        "Soweit die Inhalte in diesem Bot nicht vom Betreiber erstellt wurden, werden die Urheberrechte Dritter beachtet. Insbesondere werden Inhalte Dritter als solche gekennzeichnet. Sollten Sie trotzdem auf eine Urheberrechtsverletzung aufmerksam werden, bitten wir um einen entsprechenden Hinweis. Bei Bekanntwerden von Rechtsverletzungen werden wir derartige Inhalte umgehend entfernen.\n" +
                        "\n" +
                        "<b>Datenschutz</b>\n" +
                        "Die Betreiber dieses Bot nehmen den Schutz Ihrer persoenlichen Daten sehr ernst. Wir behandeln Ihre personenbezogenen Daten vertraulich und entsprechend der gesetzlichen Datenschutzvorschriften sowie dieser Datenschutzerklaerung.\n" +
                        "Die Nutzung unserer Webseite ist in der Regel ohne Angabe personenbezogener Daten moeglich. Soweit in unserem Bot personenbezogene Daten (beispielsweise Name, Anschrift oder E-Mail-Adressen) erhoben werden, erfolgt dies, soweit moeglich, stets auf freiwilliger Basis. Diese Daten werden ohne Ihre ausdrueckliche Zustimmung nicht an Dritte weitergegeben.\n" +
                        "Wir weisen darauf hin, dass die Datenuebertragung im Internet (z.B. bei der Kommunikation per E-Mail) Sicherheitsluecken aufweisen kann. Ein lueckenloser Schutz der Daten vor dem Zugriff durch Dritte ist nicht moeglich.\n" +
                        "\n" +
                        "<b>Recht auf Auskunft, Loeschung, Sperrung</b>\n" +
                        "Sie haben jederzeit das Recht auf unentgeltliche Auskunft ueber Ihre gespeicherten personenbezogenen Daten, deren Herkunft und Empfaenger und den Zweck der Datenverarbeitung sowie ein Recht auf Berichtigung, Sperrung oder Loeschung dieser Daten. Hierzu sowie zu weiteren Fragen zum Thema personenbezogene Daten koennen Sie sich jederzeit unter der im Impressum angegebenen Adresse an uns wenden.\n" +
                        "\n" +
                        "<b>Widerspruch Werbe-Mails</b>\n" +
                        "Der Nutzung von im Rahmen der Impressumspflicht veroeffentlichten Kontaktdaten zur Uebersendung von nicht ausdruecklich angeforderter Werbung und Informationsmaterialien wird hiermit widersprochen. Die Betreiber des Bots behalten sich ausdruecklich rechtliche Schritte im Falle der unverlangten Zusendung von Werbeinformationen, etwa durch Spam-E-Mails, vor.";
                Files.write(f.toPath(),t.getBytes());
            }
            String txt = new String(Files.readAllBytes(f.toPath()));
            send(message.getChatId(),
                    txt
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cmd_word(Message message, String[] args) {
        if (!isAdmin(message.getChatId())) {
            cmd_help(message, args);
            return;
        }
        ArrayList<String> words = new ArrayList<>();
        try {
            File f = new File("words.csv");
            f.createNewFile();
            StringTokenizer token = new StringTokenizer(new String(Files.readAllBytes(f.toPath())), ",");
            while (token.hasMoreTokens()) {
                words.add(token.nextToken());
            }
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("list")) {
                    String txt = "Wörter:\n";
                    for (int i = 0; i < words.size(); i++) {
                        txt = txt + words.get(i) + " \n";
                    }
                    send(message.getChatId(), txt);
                    return;
                } else if (args[0].equalsIgnoreCase("listformat")) {
                    String txt = "";
                    for (int i = 0; i < words.size(); i++) {
                        txt = txt + words.get(i) + " ";
                    }
                    send(message.getChatId(), txt);
                    return;
                } else if (args[0].equalsIgnoreCase("remove")) {
                    if (args.length >= 2) {
                        for (int i = 0; i < words.size(); i++) {
                            if (words.get(i).equalsIgnoreCase(args[1])) {
                                words.remove(i);
                                send(message.getChatId(), args[1] + " wurde entfernt.");
                            }
                        }
                    } else {
                        send(message.getChatId(), "Bitte gib ein Wort ein.");
                        return;
                    }
                } else {
                    for (int i = 0; i < args.length; i++) {
                        if (!words.contains(args[i]) && !args[i].isEmpty() && args[i].matches("[a-zA-Z]+")) {
                            words.add(args[i]);
                            send(message.getChatId(), args[i] + " wurde hinzgefügt.");
                        } else {
                            send(message.getChatId(), args[i] + " gibt es schon / nicht erlaubt.");
                        }
                    }
                }
                String output = "";
                for (int i = 0; i < words.size(); i++) {
                    output = output + words.get(i) + ",";
                }
                if (!output.isEmpty()) {
                    output = output.substring(0, output.length() - 1);
                    FileWriter writer = new FileWriter(f);
                    writer.write(output);
                    writer.close();
                }
            } else {
                send(message.getChatId(), "Bitte gib ein Wort oder remove/list ein.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Lobby getLobbyByUser(Long chatId) {
        for (int i = 0; i < lobbys.size(); i++) {
            if (lobbys.get(i).hasPlayer(chatId)) {
                return lobbys.get(i);
            }
        }
        return null;
    }

    private Lobby getLobbyById(int lobbyid) {
        for (int i = 0; i < lobbys.size(); i++) {
            if (lobbys.get(i).getLobbyid() == lobbyid) {
                return lobbys.get(i);
            }
        }
        return null;
    }

    private boolean createLobby(Long chatId, int lobbyid, String username) {
        if (getLobbyById(lobbyid) == null && lobbyid >= 1 && lobbyid <= MAX_LOBBYS) {
            Lobby l = new Lobby(this, chatId, lobbyid, username);
            lobbys.add(l);
            System.out.println("Lobby " + lobbyid + " erstellt");
            return true;
        }
        return false;
    }

    public void closeLobby(int lobbyid) {
        lobbys.remove(getLobbyById(lobbyid));
    }

    private void save(Object o) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(o);
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    void update(Object o) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.merge(o);
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    void remove(Object o) {
        EntityManager entityManager = sessionFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.remove(o);
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    PlayerInformation loadPlayerInformation(Long chatId) {

        try {
            EntityManager entityManager = sessionFactory.createEntityManager();
            entityManager.getTransaction().begin();
            PlayerInformation result = entityManager
                    .createQuery("FROM PlayerInformation WHERE chatId=" + chatId, PlayerInformation.class)
                    .setMaxResults(1).getSingleResult();
            entityManager.getTransaction().commit();
            entityManager.close();
            return result;
        } catch (NoResultException e) {
            PlayerInformation pi = new PlayerInformation(chatId);
            save(pi);
            return pi;
        }
    }

    void send(Long chatId, String string) {
        PlayerInformation pinfo = loadPlayerInformation(chatId);
        SendMessage message = new SendMessage(chatId, string).setParseMode("HTML");
        if (pinfo.isMute()) {
            message.disableNotification();
        } else {
            message.enableNotification();

        }
        try {
            sendMessage(message);
        } catch (TelegramApiRequestException e) {
            if (e.getErrorCode() == 403 && e.getApiResponse().contains("bot was blocked by the user")) {
                remove(pinfo);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void sendMarkup(Long chatId, String string, ReplyKeyboard markup) {
        PlayerInformation pinfo = loadPlayerInformation(chatId);
        SendMessage message = new SendMessage(chatId, string).setParseMode("HTML");
        message.setReplyMarkup(markup);
        if (pinfo.isMute()) {
            message.disableNotification();
        } else {
            message.enableNotification();

        }
        try {
            sendMessage(message);
        } catch (TelegramApiRequestException e) {
            if (e.getErrorCode() == 403 && e.getApiResponse().contains("bot was blocked by the user")) {
                remove(pinfo);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    static boolean hasOnlyEmoji(String username) {
        String txt = EmojiParser.removeAllEmojis(username);
        txt = removeAdditionalChars(txt);
        return txt.isEmpty();
    }

    public static String removeEmojis(String t) {
        String txt = EmojiParser.removeAllEmojis(t);
        txt = removeAdditionalChars(txt);
        return txt;
    }

    private static String removeAdditionalChars(String tx) {
        String txt = tx;
        for (int i = 0; i < additionalChars.length; i++) {
            txt = txt.replace("" + additionalChars[i], "");
        }
        return txt;
    }

}
