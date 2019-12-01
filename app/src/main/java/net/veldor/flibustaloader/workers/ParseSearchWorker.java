package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MainActivity;
import net.veldor.flibustaloader.ODPSActivity;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.utils.XMLParser;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static net.veldor.flibustaloader.utils.XMLParser.getDocument;

public class ParseSearchWorker extends Worker {
    public static final String ANSWER_STRING = "answer";

    public ParseSearchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        App app = App.getInstance();
        // получу ответ
        String text = app.mResponce;
        if (text != null) {
            // получу документ
            Document document = getDocument(text);
            app.mResponce = null;
            // заполню ссылку на следующую страницу
            // попробую xpath
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            Node entry;
            try {
                entry = (Node) xPath.evaluate("/feed/link[@rel='next']", document, XPathConstants.NODE);
                if (entry != null) {
                    app.mNextPageUrl = entry.getAttributes().getNamedItem("href").getNodeValue();
                } else {
                    app.mNextPageUrl = null;
                }
                // получу сущности
                NodeList entries = (NodeList) xPath.evaluate("/feed/entry", document, XPathConstants.NODESET);
                if (entries.getLength() > 0) {
                    // обработаю данные
                    switch (App.sSearchType) {
                        case ODPSActivity
                                .SEARCH_BOOKS:
                            handleBooks(entries);
                            break;
                        case ODPSActivity
                                .SEARCH_AUTHORS:
                            handleAuthors(entries);
                            break;
                    }
                } else {
                    app.mParsedResult.postValue(null);
                }

            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
        }
        return Result.success();
    }

    private void handleBooks(NodeList entries) {
        // обработаю найденные книги
        Node entry;
        FoundedBook book;
        ArrayList<FoundedBook> result = new ArrayList<>();
        int counter = 0;
        while ((entry = entries.item(counter)) != null){
            book = new FoundedBook();
            counter ++;
            result.add(book);
        }
        App.getInstance().mParsedResult.postValue(result);
    }

    private void handleAuthors(NodeList entries) {

    }

}
