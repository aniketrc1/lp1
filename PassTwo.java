import java.io.*;
import java.util.*;
import java.util.regex.*;

public class PassTwo {

    static class Symbol {
        String name;
        int address;
        Symbol(String n, int a) { name = n; address = a; }
    }

    static class Literal {
        String literal;
        int address;
        Literal(String l, int a) { literal = l; address = a; }
    }

    public static void main(String[] args) throws Exception {
        // read intermediate produced by PassOne
        BufferedReader ic = new BufferedReader(new FileReader("intermediate.txt"));
        BufferedWriter mc = new BufferedWriter(new FileWriter("machinecode.txt"));

        // Load SYMTAB and LITTAB files created by PassOne
        List<Symbol> SYMTAB = new ArrayList<>();
        List<Literal> LITTAB = new ArrayList<>();

        // Safe: if files missing, lists remain empty (but you'll want them)
        try (BufferedReader sIn = new BufferedReader(new FileReader("SYMTAB.txt"))) {
            String sLine;
            while ((sLine = sIn.readLine()) != null) {
                sLine = sLine.trim();
                if (sLine.isEmpty()) continue;
                String[] t = sLine.split("\\s+");
                SYMTAB.add(new Symbol(t[0], Integer.parseInt(t[1])));
            }
        } catch (FileNotFoundException e) {
            // warn but continue
            System.out.println("Warning: SYMTAB.txt not found — symbol addresses may be missing.");
        }

        try (BufferedReader lIn = new BufferedReader(new FileReader("LITTAB.txt"))) {
            String lLine;
            while ((lLine = lIn.readLine()) != null) {
                lLine = lLine.trim();
                if (lLine.isEmpty()) continue;
                String[] t = lLine.split("\\s+");
                LITTAB.add(new Literal(t[0], Integer.parseInt(t[1])));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Warning: LITTAB.txt not found — literal addresses may be missing.");
        }

        Pattern p = Pattern.compile("\\(([^)]*)\\)");
        String line;
        int LC = 0; // will be set by START (AD,01) if present

        while ((line = ic.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // extract all (...) contents in order
            Matcher m = p.matcher(line);
            List<String> tokens = new ArrayList<>();
            while (m.find()) {
                tokens.add(m.group(1).trim()); // e.g. "IS,04" or "1" or "L,1" or "C,200"
            }
            if (tokens.isEmpty()) continue;

            String first = tokens.get(0);

            // Assembler directive (AD)
            if (first.startsWith("AD,")) {
                // check for (C,x) among tokens
                for (String tkn : tokens) {
                    if (tkn.startsWith("C,")) {
                        String val = tkn.split(",")[1];
                        try {
                            LC = Integer.parseInt(val);
                        } catch (NumberFormatException e) { /* ignore */ }
                    }
                }
                // AD lines don't produce machine code
                continue;
            }

            // Imperative statement (IS)
            if (first.startsWith("IS,")) {
                String opcode = "00";
                String[] ff = first.split(",");
                if (ff.length > 1) opcode = ff[1];

                String reg = "0";
                int address = 0;

                // tokens may be: ["IS,04", "1", "L,1"]  OR ["IS,04", "1", "S,1"]
                // search for numeric token -> register
                for (int i = 1; i < tokens.size(); i++) {
                    String tkn = tokens.get(i);
                    if (tkn.matches("\\d+")) {
                        reg = tkn;
                        continue;
                    }
                }
                // find operand token (L,x) or (S,x)
                for (int i = 1; i < tokens.size(); i++) {
                    String tkn = tokens.get(i);
                    if (tkn.startsWith("L,")) {
                        int idx = Integer.parseInt(tkn.split(",")[1]) - 1;
                        if (idx >= 0 && idx < LITTAB.size()) address = LITTAB.get(idx).address;
                        else address = -1;
                    } else if (tkn.startsWith("S,")) {
                        int idx = Integer.parseInt(tkn.split(",")[1]) - 1;
                        if (idx >= 0 && idx < SYMTAB.size()) address = SYMTAB.get(idx).address;
                        else address = -1;
                    } else if (tkn.startsWith("C,")) {
                        address = Integer.parseInt(tkn.split(",")[1]);
                    }
                }

                // if LC still 0, set to a sensible default (200) OR use previously set; here we ensure LC positive:
                if (LC == 0) LC = 200;

                mc.write(LC + "\t" + opcode + "\t" + reg + "\t" + address + "\n");
                LC++;
                continue;
            }

            // Declarative (DL) e.g. (DL,01) (C,1)  -> allocate constant at LC
            if (first.startsWith("DL,")) {
                // find C,x token
                int constant = 0;
                for (String tkn : tokens) {
                    if (tkn.startsWith("C,")) {
                        constant = Integer.parseInt(tkn.split(",")[1]);
                        break;
                    }
                }
                if (LC == 0) LC = 200;
                mc.write(LC + "\t00\t0\t" + constant + "\n");
                LC++;
                continue;
            }

            // If line starts with something else, skip
        }

        ic.close();
        mc.close();
        System.out.println("✅ PASS 2 Completed — machinecode.txt generated successfully!");
    }
}
