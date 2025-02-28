package bg.swu.example.iot.leshan.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;

public class ClientApp {
	public static void main(String[] args) {
		// We obtained the xml files from https://github.com/OpenMobileAlliance/lwm2m-registry
		final List<ObjectModel> models = Stream.concat(
			ObjectLoader.loadDefault().stream(),
			Stream.of("/models/3.xml", "/models/3303.xml").flatMap(
			file -> {
					try {
						return ObjectLoader.loadDdfFile(
							ClientApp.class.getResourceAsStream(file), file
						).stream();
					} catch (InvalidDDFFileException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			)
		).collect(Collectors.toList());
		final LwM2mModelRepository repo = new LwM2mModelRepository(models);
	}
}

