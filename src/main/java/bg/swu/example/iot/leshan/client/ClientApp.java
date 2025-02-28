package bg.swu.example.iot.leshan.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.leshan.client.object.Device;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.client.object.Server;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;

public class ClientApp {
	public static final String DEVICE_MANUFACTURER = "SWU";
	public static final String DEVICE_MODEL_NUMBER = "test-model";
	public static final String DEVICE_SERIAL_NUMBER = "123456";

	public static final int TEMPERATURE_SENSOR_ID = 3303;

	public static void main(String[] args) {
		final String serverUri = "coap://localhost:5683";

		// The shortServerId uniquely identifies the LwM2M server for which the security
		// configuration applies. If your server uses 123 as the short server ID, use the same
		// value here. If your server uses a different short server ID, make sure to match it
		// in both the server and the client.
		final int shortServerId = 123;

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

		final List<LwM2mObjectEnabler> objectEnablers = createLwM2mObjectEnablers(
			new LwM2mModelRepository(models).getLwM2mModel(), serverUri, shortServerId
		);
	}

	private static List<LwM2mObjectEnabler> createLwM2mObjectEnablers(
		LwM2mModel model, String serverUri, int shortServerId
	) {
		final ObjectsInitializer initializer = new ObjectsInitializer(model);
		initializer.setInstancesForObject(
			LwM2mId.SECURITY, Security.noSec(serverUri, shortServerId)
		);
		initializer.setInstancesForObject(LwM2mId.SERVER, new Server(shortServerId, 300));
		initializer.setInstancesForObject(
			LwM2mId.DEVICE,
			new Device(DEVICE_MANUFACTURER, DEVICE_MODEL_NUMBER, DEVICE_SERIAL_NUMBER)
		);
		initializer.setInstancesForObject(
			TEMPERATURE_SENSOR_ID, new TemperatureSensorEnabler()
		);

		return initializer.createAll();
	}
}

