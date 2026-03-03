/*
 * Copyright (C) 2026 Ferelking (Forked from MsgGo by yztz)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package top.yztz.msggo.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Parser intelligent pour les contacts collés au format texte.
 * Supporte 3 formats:
 * 1. Numéros seuls (une par ligne)
 * 2. Nom + Numéro alternés (deux par deux)
 * 3. Format CSV/Séparé (Nom,Numéro ou Nom|Numéro)
 */
public class TextContactsParser {

    public static class ParseResult {
        public String[] titles;
        public ArrayList<HashMap<String, String>> data;
        public String detectedFormat;  // "numbers_only", "name_and_number", "csv"
        public int contactCount;

        public ParseResult(String[] titles, ArrayList<HashMap<String, String>> data, String format, int count) {
            this.titles = titles;
            this.data = data;
            this.detectedFormat = format;
            this.contactCount = count;
        }
    }

    /**
     * Parse le texte collé et détecte automatiquement le format
     */
    public static ParseResult parse(String pastedText) {
        if (pastedText == null || pastedText.trim().isEmpty()) {
            return new ParseResult(new String[]{"Numéro"}, new ArrayList<>(), "numbers_only", 0);
        }

        String[] lines = pastedText.trim().split("\\n");
        List<String> cleanedLines = new ArrayList<>();

        // Nettoyer les lignes vides
        for (String line : lines) {
            String cleaned = line.trim();
            if (!cleaned.isEmpty()) {
                cleanedLines.add(cleaned);
            }
        }

        if (cleanedLines.isEmpty()) {
            return new ParseResult(new String[]{"Numéro"}, new ArrayList<>(), "numbers_only", 0);
        }

        // Essayer de détecter le format
        // Format 1: Numéros seuls
        if (isAllPhoneNumbers(cleanedLines)) {
            return parseSimpleNumbers(cleanedLines);
        }

        // Format 2: CSV ou séparé
        if (hasSeparator(cleanedLines)) {
            return parseCsvFormat(cleanedLines);
        }

        // Format 3: Nom + Numéro alternés (pairs de lignes)
        if (isNameAndNumberFormat(cleanedLines)) {
            return parseNameAndNumber(cleanedLines);
        }

        // Par défaut: traiter comme numéros seuls
        return parseSimpleNumbers(cleanedLines);
    }

    /**
     * Vérifie si toutes les lignes sont des numéros de téléphone
     */
    private static boolean isAllPhoneNumbers(List<String> lines) {
        for (String line : lines) {
            if (!isPhoneNumber(line)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Vérifie si une ligne contient un séparateur (virgule, pipe, etc)
     */
    private static boolean hasSeparator(List<String> lines) {
        for (String line : lines) {
            if (line.contains(",") || line.contains("|") || line.contains("\t")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si le format est Nom + Numéro alternés
     * Pairs de lignes: première = nom, deuxième = numéro
     */
    private static boolean isNameAndNumberFormat(List<String> lines) {
        if (lines.size() < 2) {
            return false;
        }

        // Doit avoir un nombre pair de lignes
        if (lines.size() % 2 != 0) {
            return false;
        }

        // Vérifier que les lignes paires (indices pairs) ne sont pas des numéros
        // et que les lignes impaires (indices impairs) sont des numéros
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i % 2 == 0) {
                // Ligne paire = devrait être un nom (ne pas être un numéro)
                if (isPhoneNumber(line)) {
                    return false;
                }
            } else {
                // Ligne impaire = devrait être un numéro
                if (!isPhoneNumber(line)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Détecte si un string ressemble à un numéro de téléphone
     * Accepte flexible: 06..., +33..., 0033..., etc.
     */
    private static boolean isPhoneNumber(String s) {
        // Enlever les espaces
        s = s.replaceAll("\\s+", "");

        // Doit commencer par 0, +, ou être un nombre
        if (s.isEmpty()) {
            return false;
        }

        char firstChar = s.charAt(0);
        if (firstChar != '0' && firstChar != '+' && !Character.isDigit(firstChar)) {
            return false;
        }

        // Doit contenir majoritairement des chiffres
        int digitCount = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                digitCount++;
            }
        }

        // Au moins 7 chiffres pour un numéro de téléphone
        return digitCount >= 7;
    }

    /**
     * Parse numéros seuls (une colonne: "Numéro")
     */
    private static ParseResult parseSimpleNumbers(List<String> lines) {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();

        for (String line : lines) {
            String phoneNumber = line.trim();
            if (!phoneNumber.isEmpty()) {
                HashMap<String, String> row = new HashMap<>();
                row.put("Numéro", phoneNumber);
                data.add(row);
            }
        }

        return new ParseResult(
                new String[]{"Numéro"},
                data,
                "numbers_only",
                data.size()
        );
    }

    /**
     * Parse format Nom + Numéro alternés (pairs conseecutifs)
     * Ligne 0 = Nom, Ligne 1 = Numéro, Ligne 2 = Nom, Ligne 3 = Numéro, etc.
     */
    private static ParseResult parseNameAndNumber(List<String> lines) {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();

        for (int i = 0; i < lines.size(); i += 2) {
            if (i + 1 < lines.size()) {
                String name = lines.get(i).trim();
                String phoneNumber = lines.get(i + 1).trim();

                HashMap<String, String> row = new HashMap<>();
                row.put("Nom", name);
                row.put("Numéro", phoneNumber);
                data.add(row);
            }
        }

        return new ParseResult(
                new String[]{"Nom", "Numéro"},
                data,
                "name_and_number",
                data.size()
        );
    }

    /**
     * Parse format CSV/Séparé (Nom,Numéro ou Nom|Numéro)
     * Détecte automatiquement le séparateur
     */
    private static ParseResult parseCsvFormat(List<String> lines) {
        ArrayList<HashMap<String, String>> data = new ArrayList<>();
        String separator = detectSeparator(lines);

        for (String line : lines) {
            String[] parts = line.split("\\" + separator);

            if (parts.length == 2) {
                HashMap<String, String> row = new HashMap<>();
                String name = parts[0].trim();
                String phoneNumber = parts[1].trim();

                row.put("Nom", name);
                row.put("Numéro", phoneNumber);
                data.add(row);
            } else if (parts.length == 1) {
                // Juste un numéro
                HashMap<String, String> row = new HashMap<>();
                row.put("Numéro", parts[0].trim());
                data.add(row);
            }
        }

        String[] titles = data.isEmpty() ? new String[]{"Numéro"} : new String[]{"Nom", "Numéro"};

        return new ParseResult(
                titles,
                data,
                "csv",
                data.size()
        );
    }

    /**
     * Détecte quel séparateur est utilisé (virgule, pipe, ou tab)
     */
    private static String detectSeparator(List<String> lines) {
        int commaCount = 0;
        int pipeCount = 0;
        int tabCount = 0;

        for (String line : lines) {
            if (line.contains(",")) commaCount++;
            if (line.contains("|")) pipeCount++;
            if (line.contains("\t")) tabCount++;
        }

        if (commaCount >= pipeCount && commaCount >= tabCount) {
            return ",";
        } else if (pipeCount >= tabCount) {
            return "|";
        } else if (tabCount > 0) {
            return "\t";
        }
        return ","; // Défaut
    }
}
