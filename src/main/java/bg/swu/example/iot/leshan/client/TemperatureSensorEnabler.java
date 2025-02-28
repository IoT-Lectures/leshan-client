package bg.swu.example.iot.leshan.client;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.response.ReadResponse;

public class TemperatureSensorEnabler extends BaseInstanceEnabler {
	public static final int SENSOR_ID = 5700;

	@Override
	public ReadResponse read(LwM2mServer server, int resourceId) {
		switch (resourceId) {
			case SENSOR_ID: // Resource ID for Sensor Value
				return ReadResponse.success(resourceId, getCurrentTemperature());
			default:
				return super.read(server, resourceId);
		}
	}

	protected double getCurrentTemperature() {
		return 25.5;
	}
}
