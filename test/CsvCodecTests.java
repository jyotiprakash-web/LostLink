import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import util.CsvCodec;

public class CsvCodecTests {
    public static void run() {
        System.out.println("--- Running F21 CsvCodec Tests ---");
        try {
            // 1. Escaping rules
            TestRunner.assertEquals("Null field escapes to empty", "", CsvCodec.escape(null));
            TestRunner.assertEquals("Plain field is unquoted", "simple", CsvCodec.escape("simple"));
            TestRunner.assertEquals("Comma triggers quotes", "\"hello, world\"", CsvCodec.escape("hello, world"));
            TestRunner.assertEquals("Double-quotes are doubled and quoted", "\"She said \"\"Hi\"\"\"", CsvCodec.escape("She said \"Hi\""));
            TestRunner.assertEquals("Newline triggers quotes", "\"line1\nline2\"", CsvCodec.escape("line1\nline2"));
            TestRunner.assertEquals("CRLF triggers quotes", "\"line1\r\nline2\"", CsvCodec.escape("line1\r\nline2"));

            // 2. Row formatting
            String row1 = CsvCodec.formatRow("ITM-0001", "LOST", "Blue Backpack", "Needs, quotes", "He said \"Yes\"");
            TestRunner.assertEquals("Formatted row matches", "ITM-0001,LOST,Blue Backpack,\"Needs, quotes\",\"He said \"\"Yes\"\"\"", row1);
            TestRunner.assertEquals("Empty array format", "", CsvCodec.formatRow());
            TestRunner.assertEquals("Empty list format", "", CsvCodec.formatRow(Arrays.asList()));

            // 3. Parsing basic records
            List<String> emptyParsed = CsvCodec.parseRecord("");
            TestRunner.check("Empty record returns empty list", emptyParsed.isEmpty());

            List<String> basic = CsvCodec.parseRecord("alpha,beta,gamma");
            TestRunner.assertEquals("Basic record size is 3", 3, basic.size());
            TestRunner.assertEquals("Field 1 is alpha", "alpha", basic.get(0));
            TestRunner.assertEquals("Field 2 is beta", "beta", basic.get(1));
            TestRunner.assertEquals("Field 3 is gamma", "gamma", basic.get(2));

            List<String> emptyFields = CsvCodec.parseRecord(",middle,");
            TestRunner.assertEquals("Empty fields size is 3", 3, emptyFields.size());
            TestRunner.assertEquals("Field 1 empty", "", emptyFields.get(0));
            TestRunner.assertEquals("Field 2 middle", "middle", emptyFields.get(1));
            TestRunner.assertEquals("Field 3 empty", "", emptyFields.get(2));

            // 4. Parsing quoted fields with commas and doubled quotes
            List<String> quoted = CsvCodec.parseRecord("\"First, field\",\"Second \"\"quoted\"\" value\",Third");
            TestRunner.assertEquals("Quoted record size is 3", 3, quoted.size());
            TestRunner.assertEquals("Field with comma parsed", "First, field", quoted.get(0));
            TestRunner.assertEquals("Field with escaped quote parsed", "Second \"quoted\" value", quoted.get(1));
            TestRunner.assertEquals("Plain third field parsed", "Third", quoted.get(2));

            // 5. Roundtrip test with complex data
            List<String> original = Arrays.asList(
                    "ITM-0099",
                    "LOST",
                    "MacBook Pro, 14\"",
                    "ELECTRONICS",
                    "Space Gray",
                    "Library, 2nd Floor (Quiet Zone)",
                    "2026-03-15",
                    "Sticker on back says \"Code or Die\"!\nAlso has dent on corner.",
                    "OPEN",
                    "Student",
                    "Alice O'Connor",
                    "alice@example.com",
                    "Left near seat #42"
            );
            String encoded = CsvCodec.formatRow(original);
            List<String> decoded = CsvCodec.parseRecord(encoded);
            TestRunner.assertEquals("Roundtrip size match", original.size(), decoded.size());
            for (int i = 0; i < original.size(); i++) {
                TestRunner.assertEquals("Field " + i + " matches roundtrip", original.get(i), decoded.get(i));
            }

            // 6. Multiline reader test
            String multiLineCsv =
                    "1,Simple,Record\n" +
                    "2,\"Multiline\nDescription\nWith Lines\",End\n" +
                    "3,\"Has \"\"Quotes\"\" inside\",Final\n";
            BufferedReader reader = new BufferedReader(new StringReader(multiLineCsv));

            List<String> rec1 = CsvCodec.readNextRecord(reader);
            TestRunner.check("Rec1 not null", rec1 != null);
            TestRunner.assertEquals("Rec1 size", 3, rec1.size());
            TestRunner.assertEquals("Rec1 field 2", "Simple", rec1.get(1));

            List<String> rec2 = CsvCodec.readNextRecord(reader);
            TestRunner.check("Rec2 not null", rec2 != null);
            TestRunner.assertEquals("Rec2 size", 3, rec2.size());
            TestRunner.assertEquals("Rec2 field 1", "2", rec2.get(0));
            TestRunner.assertEquals("Rec2 multiline field preserved", "Multiline\nDescription\nWith Lines", rec2.get(1));
            TestRunner.assertEquals("Rec2 field 3", "End", rec2.get(2));

            List<String> rec3 = CsvCodec.readNextRecord(reader);
            TestRunner.check("Rec3 not null", rec3 != null);
            TestRunner.assertEquals("Rec3 size", 3, rec3.size());
            TestRunner.assertEquals("Rec3 escaped quotes", "Has \"Quotes\" inside", rec3.get(1));

            List<String> rec4 = CsvCodec.readNextRecord(reader);
            TestRunner.check("Rec4 is null at EOF", rec4 == null);

        } catch (Exception e) {
            TestRunner.check("CsvCodec tests threw unexpected exception: " + e.getMessage(), false);
        }
    }
}
