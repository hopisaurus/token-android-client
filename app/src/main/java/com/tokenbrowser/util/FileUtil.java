/*
 * 	Copyright (c) 2017. Token Browser, Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tokenbrowser.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import com.tokenbrowser.view.BaseApplication;

import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import rx.Single;
import rx.schedulers.Schedulers;

public class FileUtil {

    public static final int MAX_SIZE = 1024 * 1024;

    public Single<File> saveFileFromUri(final Context context, final Uri uri) {
        return Single.fromCallable(() -> {
            final String mimeType = context.getContentResolver().getType(uri);
            final MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            final String fileExtension = mimeTypeMap.getExtensionFromMimeType(mimeType);
            final String fileName = String.format("%s.%s", UUID.randomUUID().toString(), fileExtension);
            final File destFile = new File(BaseApplication.get().getFilesDir(), fileName);
            final InputStream inputStream = BaseApplication.get()
                    .getContentResolver()
                    .openInputStream(uri);
            return writeToFileFromInputStream(destFile, inputStream);
        })
        .subscribeOn(Schedulers.io());
    }

    private File writeToFileFromInputStream(final File file, final InputStream inputStream) throws IOException {
        final BufferedSink sink = Okio.buffer(Okio.sink(file));
        final Source source = Okio.source(inputStream);
        sink.writeAll(source);
        sink.close();
        return file;
    }

    public @Nullable File writeAttachmentToFileFromMessageReceiver(
            final SignalServiceAttachmentPointer attachment,
            final SignalServiceMessageReceiver messageReceiver) {
        File file = null;
        try {
            @SuppressLint("DefaultLocale")
            final String fileName = String.format("%d.jpg", attachment.getId());
            file = new File(BaseApplication.get().getCacheDir(), fileName);
            final InputStream stream = messageReceiver.retrieveAttachment(attachment, file);
            final File destFile = new File(BaseApplication.get().getFilesDir(), fileName);
            return writeToFileFromInputStream(destFile, stream);
        } catch (IOException | InvalidMessageException e) {
            LogUtil.exception(getClass(), "Error during writing attachment to file", e);
            return null;
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    public File createImageFileWithRandomName() {
        final String filename = UUID.randomUUID().toString() + ".jpg";
        return new File(BaseApplication.get().getFilesDir(), filename);
    }

    public String getMimeTypeFromFilename(final String filename) {
        final String fileExtension = MimeTypeMap.getFileExtensionFromUrl(filename);
        return  MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
    }

    public Single<File> compressImage(final long maxSize, final File file) {
        return Single.fromCallable(() -> {
            if (file.length() <= maxSize) return file;
            final int compressPercentage = (int)(((double)maxSize / file.length()) * 100);
            final Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            final OutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressPercentage, outputStream);
            return file;
        })
        .subscribeOn(Schedulers.io());
    }

    public String getDisplayNameFromUri(final Uri uri) {
        final String [] projection = { MediaStore.Images.Media.DISPLAY_NAME };
        final Cursor cursor =
                BaseApplication.get()
                .getContentResolver()
                .query(
                        uri,
                        projection,
                        null,
                        null,
                        null
                );

        if (cursor == null) return null;
        final int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
        cursor.moveToFirst();
        final String displayName = cursor.getString(column_index);
        cursor.close();
        return displayName;
    }
}
