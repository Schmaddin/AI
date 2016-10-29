package robothelp;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReadHelpFile {
	
	
	public static void main(String[] args) {
		readHelp(true);
	}

	public static void readHelp(boolean toIndex)
	{
		HelpFile.title = "Aeromexico";

		Path pathToHelpFile = Paths.get("knowledge.txt");
		try {
			List<String> allLines = Files.readAllLines(pathToHelpFile);
			ContentBlock currentBlock = new ContentBlock();
			for (String current : allLines) {
				if (current.startsWith("/////topic:")) {
					currentBlock = new ContentBlock();
					String edit = current.substring(11).trim();

					int id = Integer.parseInt(edit);
					System.out.println(id);
					HelpFile.mainStructure.add(currentBlock);
				}
				if (current.startsWith("///keys:")) {
					String edit = current.substring(8).trim();
					String[] keys = edit.split(",");
					System.out.println(keys);
					currentBlock.setKeys(Arrays.asList(keys));
				}
				if (current.startsWith("///title:")) {
					String edit = current.substring(9).trim();
					currentBlock.setCaption(edit);
					System.out.println(edit);
				}
				if (current.startsWith("///answer:")) {
					String edit = current.substring(10).trim();
					currentBlock.setContent(edit);
					System.out.println(edit);
				}
				if (current.startsWith("///related topic:")) {
					String edit = current.substring(17).trim();
					String[] numbersText = edit.split(",");
					int[] numbers = new int[numbersText.length];

					int i = 0;
					try {
						for (String currentNumber : numbersText) {
							numbers[i] = Integer.parseInt(numbersText[i].trim());
							i++;
						}
						System.out.println(numbers);

						currentBlock.setRef(numbers);

					} catch (NumberFormatException e) {
						currentBlock.setRef(null);
					}
				}
			}
			
			if(toIndex)
			{
			Indexer indexer=new Indexer();
			indexer.addHelp(HelpFile.mainStructure);
			indexer.close();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static class HelpFile {
		static public String title;
		static public final List<ContentBlock> mainStructure = new ArrayList<ContentBlock>();
	}

}
