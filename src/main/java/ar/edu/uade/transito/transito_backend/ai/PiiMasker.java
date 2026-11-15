package ar.edu.uade.transito.transito_backend.ai;

import java.util.regex.Pattern;

/**
 * Enmascara datos personales antes de enviar texto a un tercero (Ley 25.326):
 * emails, patentes, CUIT/CUIL, teléfonos y documentos de 7 a 11 dígitos.
 */
public final class PiiMasker {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PLATE = Pattern.compile(
            "(?<![A-Za-z0-9])(?:[A-Za-z]{2}\\s?\\d{3}\\s?[A-Za-z]{2}|[A-Za-z]{3}\\s?\\d{3})(?![A-Za-z0-9])");
    private static final Pattern CUIT = Pattern.compile("(?<!\\d)\\d{2}[-\\s]\\d{7,8}[-\\s]\\d(?!\\d)");
    private static final Pattern PHONE_INTL = Pattern.compile("\\+\\d[\\d\\s().-]{6,}\\d");
    private static final Pattern PHONE_LOCAL = Pattern.compile(
            "(?<![\\w])\\(?0?\\d{2,4}\\)?[\\s.-]\\d{3,4}[\\s.-]\\d{4}(?!\\d)");
    private static final Pattern DOTTED_ID = Pattern.compile("(?<!\\d)\\d{1,2}(?:\\.\\d{3}){2,3}(?!\\d)");
    private static final Pattern DIGIT_RUN = Pattern.compile("(?<!\\d)\\d{7,11}(?!\\d)");

    private PiiMasker() {
    }

    public static String mask(String text) {
        if (text == null) {
            return "";
        }
        String out = EMAIL.matcher(text).replaceAll("[EMAIL]");
        out = PLATE.matcher(out).replaceAll("[PATENTE]");
        out = CUIT.matcher(out).replaceAll("[DOC]");
        out = PHONE_INTL.matcher(out).replaceAll("[TEL]");
        out = PHONE_LOCAL.matcher(out).replaceAll("[TEL]");
        out = DOTTED_ID.matcher(out).replaceAll("[DOC]");
        out = DIGIT_RUN.matcher(out).replaceAll("[DOC]");
        return out;
    }
}
