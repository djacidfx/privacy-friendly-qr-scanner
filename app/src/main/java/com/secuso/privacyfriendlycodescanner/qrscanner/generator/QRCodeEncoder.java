/*
    Privacy Friendly QR Scanner
    Copyright (C) 2017-2025 Privacy Friendly QR Scanner authors and SECUSO

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.secuso.privacyfriendlycodescanner.qrscanner.generator;

/**
 * Created by bassel on 12/17/2017.
 */

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;

public class QRCodeEncoder {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private int dimension = Integer.MIN_VALUE;
    private String contents = null;
    private String displayContents = null;
    private String title = null;
    private BarcodeFormat format = null;
    private boolean encoded = false;

    public QRCodeEncoder(String data, Bundle bundle, Contents.Type type, String format, int dimension) {
        this.dimension = dimension;
        encoded = encodeContents(data, bundle, type, format);
    }

    public String getContents() {
        return contents;
    }

    public String getDisplayContents() {
        return displayContents;
    }

    public String getTitle() {
        return title;
    }

    private boolean encodeContents(String data, Bundle bundle, Contents.Type type, String formatString) {
        // Default to QR_CODE if no format given.
        format = null;
        if (formatString != null) {
            try {
                format = BarcodeFormat.valueOf(formatString);
            } catch (IllegalArgumentException iae) {
                // Ignore it then
            }
        }
        if (format == null) {
            this.format = BarcodeFormat.QR_CODE;
        }
        encodeQRCodeContents(data, bundle, type);
        return contents != null && contents.length() > 0;
    }

    private void encodeQRCodeContents(String data, Bundle bundle, Contents.Type type) {
        if (type.equals(Contents.Type.TEXT)) {
            if (data != null && data.length() > 0) {
                contents = data;
                displayContents = data;
                title = "Text";
            }
        } else if (type.equals(Contents.Type.EMAIL)) {
            data = trim(data);
            if (data != null) {
                contents = "mailto:" + data;
                displayContents = data;
                title = "E-Mail";
            }
        } else if (type.equals(Contents.Type.WEB_URL)) {
            data = trim(data);
            if (data != null) {
                if (!data.toLowerCase().startsWith("http://") && !data.toLowerCase().startsWith("https://")) {
                    contents = "http://" + data;
                } else {
                    contents = data;
                }
                displayContents = data;
                title = "URL";
            }
        } else if (type.equals(Contents.Type.PHONE)) {
            data = trim(data);
            if (data != null) {
                contents = "tel:" + data;
                displayContents = PhoneNumberUtils.formatNumber(data);
                title = "Phone";
            }

        } else if (type.equals(Contents.Type.WIFI)) {
            data = trim(data);
            if (data != null) {
                contents = "WIFI:" + data;
                // displayContents = PhoneNumberUtils.formatNumber(data);
                title = "WIFI";
            }

        } else if (type.equals(Contents.Type.ME_CARD)) {
            data = trim(data);
            if (data != null) {
                contents = "MECARD:N:" + data;
                displayContents = data;
                title = "MeCard";
            }

        } else if (type.equals(Contents.Type.BIZ_CARD)) {
            data = trim(data);
            if (data != null) {
                contents = "BIZCARD:" + data;
                displayContents = data;
                title = "BizCard";
            }
        } else if (type.equals(Contents.Type.MARKET)) {
            data = trim(data);
            if (data != null) {
                contents = "market://details?id=" + data;
                displayContents = data;
                title = "Market";
            }

        } else if (type.equals(Contents.Type.V_CARD)) {
            data = trim(data);
            if (data != null) {
                contents = "BEGIN:VCARD" + "\n" + "VERSION:3.0" + "\n" + data;
                displayContents = data;
                title = "VCard";
            }

        } else if (type.equals(Contents.Type.SMS)) {
            data = trim(data);
            if (data != null) {
                contents = "SMSTO:" + data;
                displayContents = PhoneNumberUtils.formatNumber(data);
                title = "SMS";
            }

        } else if (type.equals(Contents.Type.MMS)) {
            data = trim(data);
            if (data != null) {
                contents = "MMSTO:" + data;
                //displayContents = PhoneNumberUtils.formatNumber(data);
                title = "MMS";
            }

        } else if (type.equals(Contents.Type.CONTACT)) {
            if (bundle != null) {
                StringBuilder newContents = new StringBuilder(100);
                StringBuilder newDisplayContents = new StringBuilder(100);

                newContents.append("MECARD:");

                String name = trim(bundle.getString(ContactsContract.Intents.Insert.NAME));
                if (name != null) {
                    newContents.append("N:").append(escapeMECARD(name)).append(';');
                    newDisplayContents.append(name);
                }

                String address = trim(bundle.getString(ContactsContract.Intents.Insert.POSTAL));
                if (address != null) {
                    newContents.append("ADR:").append(escapeMECARD(address)).append(';');
                    newDisplayContents.append('\n').append(address);
                }

                Collection<String> uniquePhones = new HashSet<String>(Contents.PHONE_KEYS.length);
                for (int x = 0; x < Contents.PHONE_KEYS.length; x++) {
                    String phone = trim(bundle.getString(Contents.PHONE_KEYS[x]));
                    if (phone != null) {
                        uniquePhones.add(phone);
                    }
                }
                for (String phone : uniquePhones) {
                    newContents.append("TEL:").append(escapeMECARD(phone)).append(';');
                    newDisplayContents.append('\n').append(PhoneNumberUtils.formatNumber(phone));
                }

                Collection<String> uniqueEmails = new HashSet<String>(Contents.EMAIL_KEYS.length);
                for (int x = 0; x < Contents.EMAIL_KEYS.length; x++) {
                    String email = trim(bundle.getString(Contents.EMAIL_KEYS[x]));
                    if (email != null) {
                        uniqueEmails.add(email);
                    }
                }
                for (String email : uniqueEmails) {
                    newContents.append("EMAIL:").append(escapeMECARD(email)).append(';');
                    newDisplayContents.append('\n').append(email);
                }

                String url = trim(bundle.getString(Contents.URL_KEY));
                if (url != null) {
                    // escapeMECARD(url) -> wrong escape e.g. http\://zxing.google.com
                    newContents.append("URL:").append(url).append(';');
                    newDisplayContents.append('\n').append(url);
                }

                String note = trim(bundle.getString(Contents.NOTE_KEY));
                if (note != null) {
                    newContents.append("NOTE:").append(escapeMECARD(note)).append(';');
                    newDisplayContents.append('\n').append(note);
                }

                // Make sure we've encoded at least one field.
                if (newDisplayContents.length() > 0) {
                    newContents.append(';');
                    contents = newContents.toString();
                    displayContents = newDisplayContents.toString();
                    title = "Contact";
                } else {
                    contents = null;
                    displayContents = null;
                }

            }
        } else if (type.equals(Contents.Type.LOCATION)) {
            // if (bundle != null) {

            data = trim(data);
            if (data != null) {
                contents = "geo:" + data;
                displayContents = data;
                title = "Location";

            }
            // These must use Bundle.getFloat(), not getDouble(), it's part of the API.
              /*  float latitude = bundle.getFloat("LAT", Float.MAX_VALUE);
                float longitude = bundle.getFloat("LONG", Float.MAX_VALUE);
                if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
                    contents = "geo:" + latitude + ',' + longitude;
                    displayContents = latitude + "," + longitude;
                    title = "Location";    */
            // }
            //}
        }
    }

    public Bitmap encodeAsBitmap(String errorCorrectionLevel) throws WriterException {
        if (!encoded) return null;

        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        String encoding = guessAppropriateEncoding(contents);
        if (encoding != null) {
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        if(format.equals(BarcodeFormat.QR_CODE) || format.equals(BarcodeFormat.AZTEC) || format.equals(BarcodeFormat.PDF_417)) {
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
        }
        if (format.equals(BarcodeFormat.QR_CODE) || format.equals(BarcodeFormat.PDF_417) || format.equals(BarcodeFormat.CODE_128)) {
            hints.put(EncodeHintType.MARGIN, 0);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result = writer.encode(contents, format, dimension, dimension, hints);
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        // All are 0, or black, by default
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String result = s.trim();
        return result.length() == 0 ? null : result;
    }

    private static String escapeMECARD(String input) {
        if (input == null || (input.indexOf(':') < 0 && input.indexOf(';') < 0)) {
            return input;
        }
        int length = input.length();
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (c == ':' || c == ';') {
                result.append('\\');
            }
            result.append(c);
        }
        return result.toString();
    }
}