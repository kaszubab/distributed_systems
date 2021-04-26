import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.awt.*;
import javax.swing.*;

public class Visualizer {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String insideText = "To jest text startowy";
        JTextArea textLabel = new JTextArea();
        textLabel.setText(insideText);
        textLabel.setPreferredSize(new Dimension(600, 900));
        frame.getContentPane().add(textLabel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);

        List<String> visibleText = new LinkedList<>();
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String line = scanner.nextLine();
            visibleText.add(line);
            if (visibleText.size() > 15) {
                visibleText.remove(0);
            }
            textLabel.setText(visibleText.stream().reduce("", (text, newText) -> text + "\n " + newText));
        }
    }


}
