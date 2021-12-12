package Utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.net.URISyntaxException;

import static android.provider.DocumentsContract.isDocumentUri;

public class Common {
    @SuppressLint("NewApi")
    public static String getFilePath(Context context, Uri uri) throws URISyntaxException {
        String selection = null;
        String[]  selectionArgs = null;
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);


        if (Build.VERSION.SDK_INT >= 19 && isDocumentUri(context,uri)){
            if (isExternalStoragrDocument(uri)){
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory()+"/"+split[1];
            }
            else if (isDownloadsDocument(uri)){
                final String id = DocumentsContract.getDocumentId(uri);
//                if(!TextUtils.isEmpty(id)){
//                    if(id.startsWith("raw:")){
//                        return id.replaceFirst("raw:", "");
//                    }else if(id.startsWith("msf:")) {
//                        return id.replaceFirst("msf:", "");
//                    }
//                }
                uri  = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.parseLong(id));
                
            }
            else if (isMediaDocument(uri)){
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("image".equals(type)){
                    uri  = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                else if("video".equals(type)){
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                else if("audio".equals(type)){
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id?";
                selectionArgs = new String[] {split[1]};

        if ("content".equalsIgnoreCase(uri.getScheme())){
            String[] projection  = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()){
                    return cursor.getString(column_index);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }else if("file".equalsIgnoreCase(uri.getScheme())){
            return uri.getPath();
        }
    }
        }
        return null;

    }
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isExternalStoragrDocument(Uri uri) {
        return "com.android.externalStorage.documents".equals(uri.getAuthority());
    }

}
