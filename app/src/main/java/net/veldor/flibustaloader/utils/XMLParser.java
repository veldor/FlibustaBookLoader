package net.veldor.flibustaloader.utils;

import android.content.Intent;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.workers.ParseSearchWorker;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static net.veldor.flibustaloader.MyWebViewClient.TOR_CONNECT_ERROR_ACTION;

public class XMLParser {

    public static Document getDocument(String rawText) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(rawText));
            return dBuilder.parse(is);
        } catch (ParserConfigurationException|IOException|SAXException e) {
            // ошибка поиска, предположу, что страница недоступна
            // отправлю оповещение об ошибке загрузки TOR
            Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
            App.getInstance().sendBroadcast(finishLoadingIntent);
            e.printStackTrace();
        }
        return null;
    }

    public static void handleResults() {
        // запущу рабочего, который обработает результаты запроса
        OneTimeWorkRequest ParseSearchWorker = new OneTimeWorkRequest.Builder(ParseSearchWorker.class).build();
        WorkManager.getInstance().enqueue(ParseSearchWorker);
        App.getInstance().mSearchWork = WorkManager.getInstance().getWorkInfoByIdLiveData(ParseSearchWorker.getId());
    }
}
