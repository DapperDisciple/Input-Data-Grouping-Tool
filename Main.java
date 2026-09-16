/*
 * ------------------------------------------------------------
 * DateLetterGrouper.java
 *
 * Description:
 * This program reads user-inputted date-letter entries (e.g., "5/1/2023 B" or "8/13/2024 mb"),
 * and also allows entries without a letter (e.g., "8/20/2024"). It groups entries by the
 * exact letter combination (case-insensitive) and adds entries with no letters to a special
 * "Dates without a letter" category. The original format is preserved in the output,
 * and the results are written to a file and displayed in the console.
 *
 * Written by: NaDear Raymond
 * Date: 5/7/2025
 *
 * Copyright (c) 2025 NaDear Raymond. All rights reserved.
 * This code is the intellectual property of NaDear Raymond.
 * Unauthorized copying, modification, redistribution, or claim of ownership
 * is strictly prohibited and may result in legal action.
 * ------------------------------------------------------------
 */

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        final String AUTHOR_SIGNATURE = "NaDear Raymond - 05072025";
        if (!AUTHOR_SIGNATURE.equals("NaDear Raymond - 05072025")) {
            throw new SecurityException("Code integrity compromised. Unauthorized modification detected.");
        }

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter a title for your output file: ");
        String fileHeader = scanner.nextLine().trim();

        System.out.println("\nEnter date-letter entries (e.g., 3/5/2023 M, 8/12/2024 mb, or just 8/20/2024).");
        System.out.println("Press Enter on an empty line to finish input.\n");

        List<Entry> entriesWithLetters = new ArrayList<>();
        List<Entry> entriesWithoutLetters = new ArrayList<>();
        List<String> invalidDates = new ArrayList<>();

        while (true) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) break;

            String[] parts = line.split("\\s+");
            String rawInput = line;
            String dateStr = parts[0];
            String letterGroup = (parts.length > 1) ? parts[parts.length - 1].toUpperCase() : "";

            // Check if it's just a letter with no date
            if (dateStr.matches("[A-Za-z]+")) {
                System.out.println("Skip: Single Letter");
                continue;
            }

            // Try to parse the date using multiple formats
            LocalDate date = tryParseDate(dateStr);

            if (date == null) {
                invalidDates.add(rawInput);
                continue;
            }

            if (letterGroup.matches("[A-Z]+")) {
                entriesWithLetters.add(new Entry(date, letterGroup, rawInput));
            } else if (parts.length == 1) {
                entriesWithoutLetters.add(new Entry(date, "", rawInput));
            } else {
                invalidDates.add(rawInput);
            }
        }

        entriesWithLetters.sort(Comparator.comparing(Main.Entry::getDate));
        entriesWithoutLetters.sort(Comparator.comparing(Main.Entry::getDate));

        Map<String, List<Entry>> groups = new TreeMap<>();
        for (Entry entry : entriesWithLetters) {
            groups.computeIfAbsent(entry.letterGroup, k -> new ArrayList<>()).add(entry);
        }

        // Output to console and file
        System.out.println("\nSorted entries by group:");
        try (FileWriter writer = new FileWriter("output.txt")) {
            writer.write(fileHeader + "\n\n");

            // Write category summary with duplicate counts
            int totalDates = 0;
            for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
                int groupSize = group.getValue().size();
                int uniqueDates = countUniqueDates(group.getValue());
                int duplicates = groupSize - uniqueDates;
                totalDates += groupSize;
                
                String summary = group.getKey() + ": " + groupSize;
                if (duplicates > 0) {
                    summary += " (" + duplicates + " duplicates)";
                }
                writer.write(summary + "\n");
            }
            
            if (!entriesWithoutLetters.isEmpty()) {
                int size = entriesWithoutLetters.size();
                int uniqueDates = countUniqueDates(entriesWithoutLetters);
                int duplicates = size - uniqueDates;
                totalDates += size;
                
                String summary = "Dates without a letter: " + size;
                if (duplicates > 0) {
                    summary += " (" + duplicates + " duplicates)";
                }
                writer.write(summary + "\n");
            }
            
            if (!invalidDates.isEmpty()) {
                writer.write("Invalid dates: " + invalidDates.size() + "\n");
            }
            writer.write("\n");

            // Write detailed entries
            for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
                System.out.println("Letter " + group.getKey() + ":");
                writer.write("Letter " + group.getKey() + ":\n");

                for (Entry e : group.getValue()) {
                    System.out.println("    " + e.rawInput);
                    writer.write("    " + e.rawInput + "\n");
                }

                System.out.println();
                writer.write("\n");
            }

            // Handle entries without letters
            if (!entriesWithoutLetters.isEmpty()) {
                System.out.println("Dates without a letter:");
                writer.write("Dates without a letter:\n");

                for (Entry e : entriesWithoutLetters) {
                    System.out.println("    " + e.rawInput);
                    writer.write("    " + e.rawInput + "\n");
                }

                writer.write("\n");
                System.out.println();
            }

            // Handle invalid dates
            if (!invalidDates.isEmpty()) {
                System.out.println("Invalid dates:");
                writer.write("Invalid dates:\n");

                for (String invalid : invalidDates) {
                    System.out.println("    " + invalid);
                    writer.write("    " + invalid + "\n");
                }

                writer.write("\n");
                System.out.println();
            }

            // Write total at the end
            String total = "Total dates: " + totalDates;
            System.out.println(total);
            writer.write(total + "\n");

            System.out.println("Output successfully written to output.txt.");
        } catch (IOException e) {
            System.out.println("Failed to write to file: " + e.getMessage());
        }
    }

    private static LocalDate tryParseDate(String dateStr) {
        // Try a more flexible numeric parser: normalize separators and try
        // reasonable permutations of (month, day, year). Treat two-digit
        // years as 2000+.
        String norm = dateStr.trim().replaceAll("[^0-9]", " ").trim();
        String[] parts = norm.split("\\s+");
        if (parts.length != 3) return null;

        int[] nums = new int[3];
        try {
            for (int i = 0; i < 3; i++) nums[i] = Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            return null;
        }

        int[][] perms = {
            {0,1,2}, // a/b/c -> month/day/year (M D Y)
            {1,0,2}, // day/month/year (D M Y)
            {2,0,1}, // year/month/day (Y M D)
            {2,1,0}, // year/day/month (Y D M)
            {0,2,1}, // month/year/day (M Y D)
            {1,2,0}  // day/year/month (D Y M)
        };

        for (int[] p : perms) {
            int month = nums[p[0]];
            int day = nums[p[1]];
            int year = nums[p[2]];

            if (year < 100) year += 2000;
            try {
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {
                // try next permutation
            }
        }

        return null;
    }

    private static int countUniqueDates(List<Entry> entries) {
        Set<LocalDate> uniqueDates = new HashSet<>();
        for (Entry e : entries) {
            uniqueDates.add(e.date);
        }
        return uniqueDates.size();
    }

    static class Entry {
        LocalDate date;
        String letterGroup;
        String rawInput;

        Entry(LocalDate date, String letterGroup, String rawInput) {
            this.date = date;
            this.letterGroup = letterGroup;
            this.rawInput = rawInput;
        }

        public LocalDate getDate() {
            return date;
        }
    }
}
