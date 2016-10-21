package mi.project.core.lucene;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import mi.project.core.Indexer;
import mi.project.core.snlp.FileToSentences;

/**
 * Class to index single files into index
 * at the moment not used
 */
public class OneDocumentIndexer {
	static Indexer indexer;

	public static void recursevlyAdd(Path directory, String language) {
		System.out.println(directory.toString() + " " + directory.getFileName());

		try (DirectoryStream<Path> dir = Files.newDirectoryStream(directory)) {

			if (directory.getFileName().toString().equals("written")) {
				for (Path file : dir) {
					if (file.toAbsolutePath().toString().endsWith("txt")
							|| file.toAbsolutePath().toString().endsWith("TXT")) {
						indexer.createAndAddSentenceDoc(
								FileToSentences.returnTestSentences(file.toAbsolutePath().toString()), language,
								"-none-");
						System.out.println(file.toString());
					}
				}
			} else {
				for (Path folder : dir) {
					if (Files.isDirectory(folder)) {
						recursevlyAdd(folder, language);
					}
				}
			}
		} catch (IOException exception) {
			exception.printStackTrace();
		}

	}

}
