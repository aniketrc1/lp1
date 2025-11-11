import java.util.*;
import java.io.*;

public class PassOne {

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

        BufferedReader br = new BufferedReader(new FileReader("input.asm"));
        BufferedWriter ic = new BufferedWriter(new FileWriter("intermediate.txt"));

        Map<String, String[]> OPTAB = new HashMap<>();
        OPTAB.put("STOP", new String[] {"IS", "00"});
        OPTAB.put("ADD", new String[] {"IS", "01"});
        OPTAB.put("SUB", new String[] {"IS", "02"});
        OPTAB.put("MULT", new String[] {"IS", "03"});
        OPTAB.put("MOVER", new String[] {"IS", "04"});
        OPTAB.put("MOVEM", new String[] {"IS", "05"});
        OPTAB.put("COMP", new String[] {"IS", "06"});
        OPTAB.put("BC", new String[] {"IS", "07"});
        OPTAB.put("DIV", new String[] {"IS", "08"});
        OPTAB.put("READ", new String[] {"IS", "09"});
        OPTAB.put("PRINT", new String[] {"IS", "10"});
        OPTAB.put("START", new String[] {"AD", "01"});
        OPTAB.put("END", new String[] {"AD", "02"});
        OPTAB.put("LTORG", new String[] {"AD", "05"});
        OPTAB.put("DC", new String[] {"DL", "01"});
        OPTAB.put("DS", new String[] {"DL", "02"});

        Map<String, Integer> REGTAB = new HashMap<>();
        REGTAB.put("AREG", 1);
        REGTAB.put("BREG", 2);
        REGTAB.put("CREG", 3);
        REGTAB.put("DREG", 4);

        List<Symbol> SYMTAB = new ArrayList<>();
        List<Literal> LITTAB = new ArrayList<>();

        int LC = 0;
        String line;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] words = line.split("[ ,]+");

            if (words[0].equalsIgnoreCase("START")) {
                LC = Integer.parseInt(words[1]);
                ic.write("(AD,01)\t(C," + LC + ")\n");
                continue;
            }

            if (words[0].equalsIgnoreCase("END")) {
                for (Literal lit : LITTAB)
                    if (lit.address == -1)
                        lit.address = LC++;
                ic.write("(AD,02)\n");
                break;
            }

            if (words[0].equalsIgnoreCase("LTORG")) {
                ic.write("(AD,05)\n");
                for (Literal lit : LITTAB)
                    if (lit.address == -1)
                        lit.address = LC++;
                continue;
            }

            int startIndex = 0;
            String label = "";

            if (!OPTAB.containsKey(words[0]) && !words[0].equalsIgnoreCase("LTORG")) {
                label = words[0];
                startIndex = 1;

                boolean exists = false;
                for (Symbol s : SYMTAB) {
                    if (s.name.equals(label)) {
                        s.address = LC;
                        exists = true;
                        break;
                    }
                }
                if (!exists) SYMTAB.add(new Symbol(label, LC));
            }

            String mnemonic = words[startIndex];
            if (!OPTAB.containsKey(mnemonic)) continue;

            String[] op = OPTAB.get(mnemonic);
            String cls = op[0];
            String code = op[1];

            if (cls.equals("IS")) {
                ic.write("(" + cls + "," + code + ")\t");

                if (words.length > startIndex + 1 && REGTAB.containsKey(words[startIndex + 1])) {
                    ic.write("(" + REGTAB.get(words[startIndex + 1]) + ")");
                }

                if (words.length > startIndex + 2) {
                    String operand = words[startIndex + 2];
                    if (operand.startsWith("='")) {
                        boolean exists = false;
                        for (Literal l : LITTAB)
                            if (l.literal.equals(operand)) exists = true;
                        if (!exists) LITTAB.add(new Literal(operand, -1));
                        ic.write("(L," + LITTAB.size() + ")");
                    } else {
                        boolean exists = false;
                        for (Symbol s : SYMTAB)
                            if (s.name.equals(operand)) exists = true;
                        if (!exists) SYMTAB.add(new Symbol(operand, -1));
                        ic.write("(S," + SYMTAB.size() + ")");
                    }
                }

                ic.newLine();
                LC++;
            }

            else if (cls.equals("DL")) {

                boolean exists = false;
                for (Symbol s : SYMTAB) {
                    if (s.name.equals(label)) {
                        s.address = LC;
                        exists = true;
                        break;
                    }
                }
                if (!exists && !label.equals(""))
                    SYMTAB.add(new Symbol(label, LC));

                if (mnemonic.equalsIgnoreCase("DC")) {
                    ic.write("(DL,01)\t(C," + words[startIndex + 1] + ")\n");
                    LC++;
                } else if (mnemonic.equalsIgnoreCase("DS")) {
                    ic.write("(DL,02)\t(C," + words[startIndex + 1] + ")\n");
                    LC += Integer.parseInt(words[startIndex + 1]);
                }
            }
        }

        br.close();
        ic.close();

        BufferedWriter sOut = new BufferedWriter(new FileWriter("SYMTAB.txt"));
        for (Symbol s : SYMTAB)
            sOut.write(s.name + " " + s.address + "\n");
        sOut.close();

        BufferedWriter lOut = new BufferedWriter(new FileWriter("LITTAB.txt"));
        for (Literal l : LITTAB)
            lOut.write(l.literal + " " + l.address + "\n");
        lOut.close();

        System.out.println("PASS 1 Completed");
        //System.out.println("\nSYMBOL TABLE:");
        //for (int i = 0; i < SYMTAB.size(); i++)
            //System.out.println((i + 1) + "\t" + SYMTAB.get(i).name + "\t" + SYMTAB.get(i).address);

        //System.out.println("\nLITERAL TABLE:");
        //for (int i = 0; i < LITTAB.size(); i++)
            //System.out.println((i + 1) + "\t" + LITTAB.get(i).literal + "\t" + LITTAB.get(i).address);
    }
}