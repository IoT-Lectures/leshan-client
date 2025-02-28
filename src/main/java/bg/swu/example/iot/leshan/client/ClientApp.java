package bg.swu.example.iot.leshan.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.LeshanClientBuilder;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
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
import org.eclipse.leshan.transport.javacoap.client.coaptcp.endpoint.JavaCoapTcpClientEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.client.coaptcp.endpoint.JavaCoapsTcpClientEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.client.endpoint.JavaCoapClientEndpointsProvider;

public class ClientApp {
	public static final String DEVICE_MANUFACTURER = "SWU";
	public static final String DEVICE_MODEL_NUMBER = "test-model";
	public static final String DEVICE_SERIAL_NUMBER = "123456";

	public static final String ENDPOINT = "swu-client";
	public static final int TEMPERATURE_SENSOR_ID = 3303;

	public static void main(String[] args) {
		final String serverUri = "coap://localhost:5683";
		final String serverTcpUri = "coap+tcp://localhost:5683";

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

		// Use serverTcpUri if you want the connection to be made over TCP/IP.
		final List<LwM2mObjectEnabler> objectEnablers = createLwM2mObjectEnablers(
			new LwM2mModelRepository(models).getLwM2mModel(), serverUri, shortServerId
		);

		final LeshanClientBuilder builder = new LeshanClientBuilder(ENDPOINT);
		builder.setObjects(objectEnablers);
		final List<LwM2mClientEndpointsProvider> endpointsProviders = new ArrayList<>();
		endpointsProviders.add(new JavaCoapClientEndpointsProvider());
		endpointsProviders.add(new JavaCoapTcpClientEndpointsProvider());
		endpointsProviders.add(new JavaCoapsTcpClientEndpointsProvider());
		builder.setEndpointsProviders(endpointsProviders);
        final LeshanClient client = builder.build();

		client.start();
		System.out.println("LwM2M Client started...");

		final Runnable destroyOp = () -> {
			System.out.println("Stopping LwM2M Client...");
			// Deregister from the server before shutting down. We use destroy, because stop
			// method simply stops the client but does not notify the LwM2M server.
			client.destroy(true);
		};

		// Keep running
		Runtime.getRuntime().addShutdownHook(new Thread(destroyOp));
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

