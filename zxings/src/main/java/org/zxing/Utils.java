package org.zxing;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Hashtable;

public final class Utils {
    public Utils() {
    }

    /**
     * 判断是否支持打开手电筒
     *
     * @param pm
     * @return
     */
    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f != null && PackageManager.FEATURE_CAMERA_FLASH.equals(f.name))
                        return true;
                }
            }
        }
        return false;
    }

    public static boolean isChineseCharacter(String chineseStr) {
        char[] charArray = chineseStr.toCharArray();

        for (int i = 0; i < charArray.length; ++i) {
            if ((charArray[i] < 0 || charArray[i] >= '�') && (charArray[i] <= '�' || charArray[i] >= '\uffff')) {
                return false;
            }
        }

        return true;
    }

    public static Drawable generateQRCode(Context context, String info, int[] size) {
        try {
            Hashtable e = new Hashtable();
            e.put(EncodeHintType.CHARACTER_SET, "utf-8");
            int qrWidth = size[0];
            int qrHeith = size[1];
            BitMatrix bitMatrix = (new QRCodeWriter()).encode(info, BarcodeFormat.QR_CODE, qrWidth, qrHeith, e);
            int[] pixels = new int[qrWidth * qrHeith];

            for (int bitmap = 0; bitmap < qrHeith; ++bitmap) {
                for (int x = 0; x < qrWidth; ++x) {
                    if (bitMatrix.get(x, bitmap)) {
                        pixels[bitmap * qrWidth + x] = -16777216;
                    } else {
                        pixels[bitmap * qrWidth + x] = -1;
                    }
                }
            }

            Bitmap var11 = Bitmap.createBitmap(qrWidth, qrHeith, Bitmap.Config.ARGB_8888);
            var11.setPixels(pixels, 0, qrWidth, 0, 0, qrWidth, qrHeith);
            return new BitmapDrawable(context.getResources(), var11);
        } catch (WriterException var10) {
            var10.printStackTrace();
            return null;
        }
    }

    @SuppressLint({"NewApi"})
    public static String getPath(Context context, Uri uri) {
        boolean isKitKat = Build.VERSION.SDK_INT >= 19;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            String docId;
            String[] split;
            String type;
            if (isExternalStorageDocument(uri)) {
                docId = DocumentsContract.getDocumentId(uri);
                split = docId.split(":");
                type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else {
                if (isDownloadsDocument(uri)) {
                    docId = DocumentsContract.getDocumentId(uri);
                    Uri split1 = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId).longValue());
                    return getDataColumn(context, split1, (String) null, (String[]) null);
                }

                if (isMediaDocument(uri)) {
                    docId = DocumentsContract.getDocumentId(uri);
                    split = docId.split(":");
                    type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    String selection = "_id=?";
                    String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, "_id=?", selectionArgs);
                }
            }
        } else {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, (String) null, (String[]) null);
            }

            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = new String[]{"_data"};

        String var8;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, (String) null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            int column_index = cursor.getColumnIndexOrThrow("_data");
            var8 = cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }

        }

        return var8;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
