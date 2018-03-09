import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class TransformRepoSlugToRepoId {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage: TransformRepoSlugToRepoId <input> <output>");
			System.exit(-1);
		}

		File input = new File(args[0]);
		File output = new File(args[1]);
		if (output.exists()) {
			System.err.println("Please give the path of a file that does not exist for output: "+args[1]);
			System.exit(-1);
		}
		if (input.exists()) {
			List<String> slugs = Files.readAllLines(input.toPath());
			FileWriter fw = new FileWriter(output);

			int i = 0;
			for (String slug : slugs) {
				Repository repo = RepositoryHelper.getRepositoryFromSlug(slug);
				if (repo != null) {
					fw.append(repo.getId()+"");
					fw.append("\n");
					fw.flush();
					i++;
				}
			}

			fw.close();
			System.out.println(i+" entries written in "+args[1]);
		} else {
			System.err.println("The given input files does not exist: "+args[0]);
		}

	}
}
