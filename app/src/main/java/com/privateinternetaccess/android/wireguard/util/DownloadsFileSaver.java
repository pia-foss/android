/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.wireguard.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DownloadsFileSaver {

    public static class DownloadsFile {
        private Context context;
        private OutputStream outputStream;
        private String fileName;
        private Uri uri;

        private DownloadsFile(final Context context, final OutputStream outputStream, final String fileName, final Uri uri) {
            this.context = context;
            this.outputStream = outputStream;
            this.fileName = fileName;
            this.uri = uri;
        }

        public OutputStream getOutputStream() { return outputStream; }
        public String getFileName() { return fileName; }

        public void delete() {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
//                context.getContentResolver().delete(uri, null, null);
//            else
                new File(fileName).delete();
        }
    }

    public static DownloadsFile save(final Context context, final String name, final String mimeType, final boolean overwriteExisting) throws Exception {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            final ContentResolver contentResolver = context.getContentResolver();
//            if (overwriteExisting)
//                contentResolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.format("%s = ?", MediaColumns.DISPLAY_NAME), new String[]{name});
//            final ContentValues contentValues = new ContentValues();
//            contentValues.put(MediaColumns.DISPLAY_NAME, name);
//            contentValues.put(MediaColumns.MIME_TYPE, mimeType);
//            final Uri contentUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
//            if (contentUri == null)
//                throw new IOException(context.getString(R.string.create_downloads_file_error));
//            final OutputStream contentStream = contentResolver.openOutputStream(contentUri);
//            if (contentStream == null)
//                throw new IOException(context.getString(R.string.create_downloads_file_error));
//            @SuppressWarnings("deprecation")
//            Cursor cursor = contentResolver.query(contentUri, new String[]{MediaColumns.DATA}, null, null, null);
//            String path = null;
//            if (cursor != null) {
//                try {
//                    if (cursor.moveToFirst())
//                        path = cursor.getString(0);
//                } finally {
//                    cursor.close();
//                }
//            }
//            if (path == null) {
//                path = "Download/";
//                cursor = contentResolver.query(contentUri, new String[]{MediaColumns.DISPLAY_NAME}, null, null, null);
//                if (cursor != null) {
//                    try {
//                        if (cursor.moveToFirst())
//                            path += cursor.getString(0);
//                    } finally {
//                        cursor.close();
//                    }
//                }
//            }
//            return new DownloadsFile(context, contentStream, path, contentUri);
//        }
//        else {
            @SuppressWarnings("deprecation")
            final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            final File file = new File(path, name);
            if (!path.isDirectory() && !path.mkdirs())
                throw new IOException("Cannot create output directory");
            return new DownloadsFile(context, new FileOutputStream(file), file.getAbsolutePath(), null);
//        }
    }
}
