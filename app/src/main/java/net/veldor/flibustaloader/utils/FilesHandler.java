package net.veldor.flibustaloader.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static net.veldor.flibustaloader.utils.BookOpener.intentCanBeHandled;

public class FilesHandler {
    public static void shareFile(File zip) {
        //todo По возможности- разобраться и заменить на валидное решение
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        // отправлю запрос на открытие файла
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zip));
        shareIntent.setType(getMimeType(zip.getName()));
        shareIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK);
        App.getInstance().startActivity(Intent.createChooser(shareIntent, App.getInstance().getString(R.string.send_book_title)).addFlags(FLAG_ACTIVITY_NEW_TASK));

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void shareFile(DocumentFile incomingDocumentFile) {
        String mime = getMimeType(incomingDocumentFile.getName());
        if (mime != null && !mime.isEmpty()) {
            if (!mime.equals("application/x-mobipocket-ebook")) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, incomingDocumentFile.getUri());
                shareIntent.setType(getMimeType(incomingDocumentFile.getName()));
                shareIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK);
                App.getInstance().startActivity(Intent.createChooser(shareIntent, App.getInstance().getString(R.string.send_book_title)).addFlags(FLAG_ACTIVITY_NEW_TASK));
            } else {
                String docId = DocumentsContract.getDocumentId(incomingDocumentFile.getUri());
                final String[] split = docId.split(":");
                final String storage = split[0];
                String path = "///storage/" + storage + "/" + split[1];
                // получу файл из documentFile и отправлю его
                File file = getFileFromDocumentFile(incomingDocumentFile);
                if (file != null) {
                    if (!file.exists()) {
                        file = new File(Environment.getExternalStorageDirectory() + "/" + split[1]);
                    }
                    if (file.exists()) {
                        //todo По возможности- разобраться и заменить на валидное решение
                        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                        StrictMode.setVmPolicy(builder.build());
                        // отправлю запрос на открытие файла
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        shareIntent.setType(mime);
                        shareIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK);
                        App.getInstance().startActivity(Intent.createChooser(shareIntent, App.getInstance().getString(R.string.send_book_title)).addFlags(FLAG_ACTIVITY_NEW_TASK));
                    }
                }
            }
        }
    }

    private static File getFileFromDocumentFile(DocumentFile df) {
        final String docId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            docId = DocumentsContract.getDocumentId(df.getUri());
            final String[] split = docId.split(":");
            final String storage = split[0];
            String path = "///storage/" + storage + "/" + split[1];
            return new File(path);
        }
        return null;
    }

    // url = file path or whatever suitable URL you want.
    public static String getMimeType(String url) {
        if (url != null && !url.isEmpty()) {
            String extension = url.substring(url.lastIndexOf(".") + 1);
            return MimeTypes.getFullMime(extension);
        }
        return null;
    }

    public static String getChangeText() {
        try {
            InputStream textFileStream = App.getInstance().getAssets().open("   changes.txt");
            BufferedReader r = new BufferedReader(new InputStreamReader(textFileStream));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append('\u25CF').append(' ').append(line).append("\n\n");
            }
            return total.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "no changes";
    }

    public static DocumentFile getDownloadFile(BooksDownloadSchedule book, HttpResponse response) {
        boolean extensionSetted = false;

//        Header[] headers = response.getAllHeaders();
//        for (Header h :
//                headers) {
//            Log.d("surprise", "FilesHandler: 127 " + h.getName());
//            Log.d("surprise", "FilesHandler: 127 " + h.getValue());
//        }

        // try to get file name with extension
        Header filenameHeader = response.getLastHeader("Content-Disposition");
        if(filenameHeader != null){
            String value = filenameHeader.getValue();
            if(value != null){
                String extension = Grammar.getExtension(value.replace("\"", ""));
                // setup extension
                book.format = MimeTypes.getFullMime(extension);
                book.name = Grammar.changeExtension(book.name, extension);
//                Log.d("surprise", "*******FilesHandler: v1 EXTENSION " + extension);
//                Log.d("surprise", "*******FilesHandler: v1 MIME " + book.format);
//                Log.d("surprise", "*******FilesHandler: v1 CONVERTED NAME " + book.name);
                extensionSetted = true;
            }
        }
        if(!extensionSetted){
            Header receivedContentType = response.getLastHeader("Content-Type");
            String trueFormat = receivedContentType.getValue();
            String trueFormatExtension = MimeTypes.getTrueFormatExtension(trueFormat);
            if(trueFormatExtension != null) {
                book.format = trueFormat;
                book.name = Grammar.changeExtension(book.name, trueFormatExtension);
//                Log.d("surprise", "*******FilesHandler: v2 EXTENSION " + trueFormatExtension);
//                Log.d("surprise", "*******FilesHandler: v2 MIME " + book.format);
//                Log.d("surprise", "*******FilesHandler: v2 CONVERTED NAME " + book.name);
            }
        }
        // получу имя файла
        DocumentFile downloadsDir = App.getInstance().getDownloadDir();
        if (MyPreferences.getInstance().isCreateSequencesDir() && book.reservedSequenceName != null) {
            if (MyPreferences.getInstance().isCreateAdditionalDir()) {
                DocumentFile seriesDir = downloadsDir.findFile("Серии");
                if (seriesDir == null || !seriesDir.exists()) {
                    downloadsDir = downloadsDir.createDirectory("Серии");
                } else {
                    downloadsDir = seriesDir;
                }
            }
            if (downloadsDir != null && downloadsDir.findFile(book.reservedSequenceName) == null) {
                downloadsDir = downloadsDir.createDirectory(book.reservedSequenceName);
            } else if (downloadsDir != null) {
                downloadsDir = downloadsDir.findFile(book.reservedSequenceName);
            }
            if (downloadsDir == null) {
                downloadsDir = App.getInstance().getDownloadDir();
            }
        } else {
            // проверю, нужно ли создавать папку под автора
            if (MyPreferences.getInstance().isCreateAuthorsDir()) {
                if (MyPreferences.getInstance().isCreateAdditionalDir()) {
                    DocumentFile authorsDir = downloadsDir.findFile("Авторы");
                    if (authorsDir == null || !authorsDir.exists()) {
                        downloadsDir = downloadsDir.createDirectory("Авторы");
                    } else {
                        downloadsDir = authorsDir;
                    }
                }
                // создам папку
                if (downloadsDir != null && downloadsDir.findFile(book.authorDirName) == null) {
                    downloadsDir = downloadsDir.createDirectory(book.authorDirName);
                } else if (downloadsDir != null) {
                    downloadsDir = downloadsDir.findFile(book.authorDirName);
                }
                if (downloadsDir == null) {
                    downloadsDir = App.getInstance().getDownloadDir();
                }
            }
            if (MyPreferences.getInstance().isCreateSequencesDir() && book.sequenceDirName != null && !book.sequenceDirName.isEmpty()) {
                if (downloadsDir.findFile(book.sequenceDirName) == null) {
                    downloadsDir = downloadsDir.createDirectory(book.sequenceDirName);
                } else {
                    downloadsDir = downloadsDir.findFile(book.sequenceDirName);
                }
                if (downloadsDir == null) {
                    downloadsDir = App.getInstance().getDownloadDir();
                }
            }
        }
        // проверю, нет ли ещё файла с таким именем, если есть- удалю
        DocumentFile[] files = downloadsDir.listFiles();
        int fileCounter = 0;
        DocumentFile oneFile;
        if (files.length > 0) {
            while (fileCounter < files.length - 1) {
                fileCounter++;
                oneFile = files[fileCounter];
                if (oneFile.isFile()) {
                    if (Objects.requireNonNull(oneFile.getName()).startsWith(book.name)) {
                        oneFile.delete();
                    }
                }
            }
        }
        DocumentFile existentFile = downloadsDir.findFile(book.name);
        if (existentFile != null) {
            existentFile.delete();
        }
        DocumentFile file = downloadsDir.createFile(book.format, book.name);
        Log.d("surprise", "FilesHandler: 217 created file " + file.getName());
        return file;
    }

    public static File getCompatDownloadFile(BooksDownloadSchedule book) {
        File file = MyPreferences.getInstance().getDownloadDir();
        // проверю, нужно ли создавать папку под автора
        if (MyPreferences.getInstance().isCreateSequencesDir() && book.reservedSequenceName != null) {
            file = new File(file, book.reservedSequenceName);
        } else {
            if (MyPreferences.getInstance().isCreateAuthorsDir()) {
                file = new File(file, book.authorDirName);
            }
            if (MyPreferences.getInstance().isCreateSequencesDir() && book.sequenceDirName != null) {
                file = new File(file, book.sequenceDirName);
            }
        }
        return new File(file, book.name);
    }

    public static File getBaseDownloadFile(BooksDownloadSchedule book) {
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(file, book.name);
    }

    public static void openFile(DocumentFile file) {
        String mime = getMimeType(file.getName());
        if (mime != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(file.getUri(), mime);
            intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK);
            App.getInstance().startActivity(Intent.createChooser(intent, App.getInstance().getString(R.string.open_with_menu_item)).addFlags(FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public static boolean isBookDownloaded(BooksDownloadSchedule newBook) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
            if (MyPreferences.getInstance().isCreateSequencesDir() && newBook.reservedSequenceName != null) {
                if (MyPreferences.getInstance().isCreateAdditionalDir()) {
                    DocumentFile sequencesDir = downloadsDir.findFile("Серии");
                    if (sequencesDir != null && sequencesDir.exists()) {
                        downloadsDir = sequencesDir;
                    }
                }
                downloadsDir = downloadsDir.findFile(newBook.reservedSequenceName);
            } else {
                if (MyPreferences.getInstance().isCreateAuthorsDir() && newBook.authorDirName != null && !newBook.authorDirName.isEmpty()) {
                    DocumentFile sequencesDir = downloadsDir.findFile("Авторы");
                    if (sequencesDir != null && sequencesDir.exists()) {
                        downloadsDir = sequencesDir;
                    }
                    downloadsDir = downloadsDir.findFile(newBook.authorDirName);
                }
                if (MyPreferences.getInstance().isCreateSequencesDir() && downloadsDir != null && newBook.sequenceDirName != null && newBook.reservedSequenceName != null && !newBook.reservedSequenceName.isEmpty()) {
                    downloadsDir = downloadsDir.findFile(newBook.sequenceDirName);
                }
            }
            if (downloadsDir != null) {
                DocumentFile file = downloadsDir.findFile(newBook.name);
                if (file != null && file.isFile() && file.canRead() && file.length() > 0) {
                    return true;
                }
            }
        } else {
            File dd = MyPreferences.getInstance().getDownloadDir();
            if (MyPreferences.getInstance().isCreateAuthorsDir() && newBook.authorDirName != null && !newBook.authorDirName.isEmpty()) {
                dd = new File(dd, newBook.authorDirName);
            }
            if (MyPreferences.getInstance().isCreateSequencesDir() && newBook.sequenceDirName != null && !newBook.sequenceDirName.isEmpty()) {
                dd = new File(dd, newBook.sequenceDirName);
            }
            File file = new File(dd, newBook.name);
            if (file.isFile() && file.canRead() && file.length() > 0) {
                return true;
            }
        }
        return false;
    }
}
