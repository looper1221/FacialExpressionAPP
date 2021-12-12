package Utils;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.pct.moodymusic3.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {

    private File file;
    private IUploadCallbacks listener = new IUploadCallbacks() {
        @Override
        public void onProgressUpdate(int percent) {

        }
    };
    private static int DEFAULT_BUFFER_SIZE = 4096;

//    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

    public ProgressRequestBody(File file, MainActivity mainActivity) {
        this.file = file;
        System.out.println("Filelelel: " + file.length());
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("image/*");
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = file.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        FileInputStream in = new FileInputStream(file);
        long uploaded = 0;
        try{
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1){
                handler.post(new ProgressUpdater(uploaded, fileLength));
                uploaded+=read;
                sink.write(buffer,0,read);

            }
        }finally {
            in.close();
        }
    }

    private class ProgressUpdater implements Runnable {
        private long uploaded;
        private long fileLength;

        public ProgressUpdater(long uploaded, long fileLength) {
            this.uploaded = uploaded;
            this.fileLength = fileLength;
        }

        @Override
        public void run() {
            System.out.println(uploaded+"::::"+fileLength);
            listener.onProgressUpdate((int)((100*uploaded)/fileLength));
        }
    }
}
