/*-
 * Copyright (c) 2016 Coteq, Johan Cosemans
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY COTEQ AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.yourhome.server.net.rest.view;

import net.yourhome.common.base.enums.Alignments;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.base.enums.ViewTypes;
import net.yourhome.common.net.model.viewproperties.*;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;
import net.yourhome.server.net.Server;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageHelper {

	public static final String IPCAMSNAPSHOTS = "/ipcamerasnapshots";
	public static final String HOMEDESIGNER_PATH = "/YourHomeDesigner";
	public static final String IMAGE_FOLDER = ImageHelper.HOMEDESIGNER_PATH + "/images";
	public static final String USER_IMAGES = ImageHelper.IMAGE_FOLDER + "/User";
	public static final String USER_BACKGROUND_IMAGES = ImageHelper.USER_IMAGES + "/backgrounds";

	private static Logger log = Logger.getLogger(ImageHelper.class);
	private JSONArray imageArray = null;

	// Singleton
	private static ImageHelper instance;

	private ImageHelper() {
	};

	public static ImageHelper getInstance() {
		if (ImageHelper.instance == null) {
			ImageHelper.instance = new ImageHelper();
		}

		return ImageHelper.instance;
	}

	public List<ViewGroup> getViewGroups() {

		List<ViewGroup> returnList = new ArrayList<ViewGroup>();

		/* Light */
		ViewGroup lightGroup = new ViewGroup();
		lightGroup.setTitle("Light");

		MultiStateButton lightButton = new MultiStateButton("lightButton", ImageHelper.IMAGE_FOLDER + "/Light/light_dim2.png", ImageHelper.IMAGE_FOLDER + "/Light/light_dim2.png", "Light Button (Dimmer)");
		lightButton.addState("On", "99", ImageHelper.IMAGE_FOLDER + "/Light/light_on.png");
		lightButton.addState("Dim", "40", ImageHelper.IMAGE_FOLDER + "/Light/light_dim2.png");
		lightButton.addState("Off", "0", ImageHelper.IMAGE_FOLDER + "/Light/light_off.png");
		lightButton.addAllowed(new ValueTypes[] { ValueTypes.DIMMER });
		lightGroup.addView(lightButton);

		MultiStateButton lightButton2 = new MultiStateButton("lightButton2", ImageHelper.IMAGE_FOLDER + "/Light/light_on.png", ImageHelper.IMAGE_FOLDER + "/Light/light_on.png", "Light Button (Binary switch)");
		lightButton2.addState("On", "true", ImageHelper.IMAGE_FOLDER + "/Light/light_on.png");
		lightButton2.addState("Off", "false", ImageHelper.IMAGE_FOLDER + "/Light/light_off.png");
		lightButton2.addAllowed(new ValueTypes[] { ValueTypes.SWITCH_BINARY, ValueTypes.DIMMER });
		lightGroup.addView(lightButton2);

		MultiStateButton lightButtonbw = new MultiStateButton("lightButtonbw", ImageHelper.IMAGE_FOLDER + "/Light/light-dimmed.png", ImageHelper.IMAGE_FOLDER + "/Light/light-dimmed.png", "Light Button b&w (Dimmer)");
		lightButtonbw.addState("On", "99", ImageHelper.IMAGE_FOLDER + "/Light/light-white.png");
		lightButtonbw.addState("Dim", "40", ImageHelper.IMAGE_FOLDER + "/Light/light-dimmed.png");
		lightButtonbw.addState("Off", "0", ImageHelper.IMAGE_FOLDER + "/Light/light-black.png");
		lightButtonbw.addAllowed(new ValueTypes[] { ValueTypes.DIMMER });
		lightGroup.addView(lightButtonbw);

		MultiStateButton lightButton2bw = new MultiStateButton("lightButton2bw", ImageHelper.IMAGE_FOLDER + "/Light/light-white.png", ImageHelper.IMAGE_FOLDER + "/Light/light-white.png", "Light Button b&w (Binary switch)");
		lightButton2bw.addState("On", "true", ImageHelper.IMAGE_FOLDER + "/Light/light-white.png");
		lightButton2bw.addState("Off", "false", ImageHelper.IMAGE_FOLDER + "/Light/light-black.png");
		lightButton2bw.addAllowed(new ValueTypes[] { ValueTypes.SWITCH_BINARY, ValueTypes.DIMMER });
		lightGroup.addView(lightButton2bw);

		lightGroup.addView(new Slider("slider", ImageHelper.IMAGE_FOLDER + "/Light/slider_icon.png", ImageHelper.IMAGE_FOLDER + "/Light/slider.png", "", 99.0, 0.0).addAllowed(new ValueTypes[] { ValueTypes.DIMMER }));

		MultiStateButton onOffSwitch = new MultiStateButton("onOffSwitch", ImageHelper.IMAGE_FOLDER + "/Light/switch_on-128-round_on.png", ImageHelper.IMAGE_FOLDER + "/Light/switch_on-128-round_on.png", "On/Off Switch");
		onOffSwitch.addState("On", "true", ImageHelper.IMAGE_FOLDER + "/Light/switch_on-128-round_on.png");
		onOffSwitch.addState("Off", "false", ImageHelper.IMAGE_FOLDER + "/Light/switch_off-128-round_off.png");
		onOffSwitch.addAllowed(new ValueTypes[] { ValueTypes.SENSOR_MOTION, ValueTypes.SENSOR_BINARY, ValueTypes.SWITCH_BINARY });
		lightGroup.addView(onOffSwitch);

		MultiStateButton onOffSwitch2 = new MultiStateButton("onOffSwitch2", ImageHelper.IMAGE_FOLDER + "/Light/switch_on-128-rect_on.png", ImageHelper.IMAGE_FOLDER + "/Light/switch_on-128-rect_on.png", "On/Off Switch 2");
		onOffSwitch2.addState("On", "true", ImageHelper.IMAGE_FOLDER + "/Light/switch_on-128-rect_on.png");
		onOffSwitch2.addState("Off", "false", ImageHelper.IMAGE_FOLDER + "/Light/switch_off-128-rect_off.png");
		onOffSwitch2.addAllowed(new ValueTypes[] { ValueTypes.SENSOR_MOTION, ValueTypes.SENSOR_BINARY, ValueTypes.SWITCH_BINARY });
		lightGroup.addView(onOffSwitch2);

		ColorPicker colorPicker = new ColorPicker("colorPicker", ImageHelper.IMAGE_FOLDER + "/Light/ColorPicker.png", ImageHelper.IMAGE_FOLDER + "/Light/ColorPicker.png", "Color Picker");
		colorPicker.addAllowed(new ValueTypes[] { ValueTypes.COLOR_BULB });
		lightGroup.addView(colorPicker);

		returnList.add(lightGroup);

		/* Sensor */
		ViewGroup sensorGroup = new ViewGroup();
		sensorGroup.setTitle("Sensor");
		// sensorGroup.addView(new Image("energieTitleIcon",IMAGE_FOLDER +
		// "/Sensor/energie_title_icon.png", IMAGE_FOLDER +
		// "/Sensor/energie_title.png", "Energy Header", IMAGE_FOLDER +
		// "/Sensor/energie_title.png").addAllowed(new ValueTypes[] {}));
		sensorGroup.addView(new Sensor("generalSensor", ImageHelper.IMAGE_FOLDER + "/Sensor/general_sensor_icon.png", ImageHelper.IMAGE_FOLDER + "/Sensor/general_sensor.png", "General sensor or meter", "#ffffff", 20.0).addAllowed(new ValueTypes[] { ValueTypes.SENSOR_GENERAL, ValueTypes.SENSOR_HUMIDITY, ValueTypes.SENSOR_LUMINOSITY, ValueTypes.SENSOR_TEMPERATURE, ValueTypes.METER, ValueTypes.HEATING }));
		sensorGroup.addView(new SensorWithIndicator("humiditySensor", ImageHelper.IMAGE_FOLDER + "/Sensor/humidity_icon.png", ImageHelper.IMAGE_FOLDER + "/Sensor/humidity_example.png", "Sensor Images", ImageHelper.IMAGE_FOLDER + "/Sensor/humidity_more.png", ImageHelper.IMAGE_FOLDER + "/Sensor/humidity_neutral.png", ImageHelper.IMAGE_FOLDER + "/Sensor/humidity_less.png", "#ffffff", 20.0).addAllowed(new ValueTypes[] { ValueTypes.SENSOR_GENERAL, ValueTypes.SENSOR_HUMIDITY }));

		sensorGroup.addView(new SensorWithIndicator("luminositySensor", ImageHelper.IMAGE_FOLDER + "/Sensor/luminosity_icon.png", ImageHelper.IMAGE_FOLDER + "/Sensor/luminosity_example.png", "Sensor Images", ImageHelper.IMAGE_FOLDER + "/Sensor/luminosity_more.png", ImageHelper.IMAGE_FOLDER + "/Sensor/luminosity_neutral.png", ImageHelper.IMAGE_FOLDER + "/Sensor/luminosity_less.png", "#ffffff", 20.0).addAllowed(new ValueTypes[] { ValueTypes.SENSOR_GENERAL, ValueTypes.SENSOR_LUMINOSITY }));

		sensorGroup.addView(new SensorWithIndicator("temperatureSensor", ImageHelper.IMAGE_FOLDER + "/Sensor/temperature_icon.png", ImageHelper.IMAGE_FOLDER + "/Sensor/temperature_example.png", "Sensor Images", ImageHelper.IMAGE_FOLDER + "/Sensor/temperature_more.png", ImageHelper.IMAGE_FOLDER + "/Sensor/temperature_neutral.png", ImageHelper.IMAGE_FOLDER + "/Sensor/temperature_less.png", "#ffffff", 20.0).addAllowed(new ValueTypes[] { ValueTypes.SENSOR_GENERAL, ValueTypes.SENSOR_TEMPERATURE, ValueTypes.HEATING }));

		MultiStateButton motionDetector = new MultiStateButton("motionDetector", ImageHelper.IMAGE_FOLDER + "/Sensor/motion_detector-128-light_on.png", ImageHelper.IMAGE_FOLDER + "/Sensor/motion_detector-128-light_on.png", "Motion");
		motionDetector.addState("Motion", "true", ImageHelper.IMAGE_FOLDER + "/Sensor/motion_detector-128-light_on.png");
		motionDetector.addState("No Motion", "false", ImageHelper.IMAGE_FOLDER + "/Sensor/motion_detector-128-light_off.png");
		motionDetector.addAllowed(new ValueTypes[] { ValueTypes.SENSOR_MOTION, ValueTypes.SENSOR_BINARY });
		sensorGroup.addView(motionDetector);

		MultiStateButton doorSensorLeft = new MultiStateButton("doorSensorLeft", ImageHelper.IMAGE_FOLDER + "/Sensor/door_left_open.png", ImageHelper.IMAGE_FOLDER + "/Sensor/door_left_open.png", "Door Sensor (left)");
		doorSensorLeft.addState("Open", "true", ImageHelper.IMAGE_FOLDER + "/Sensor/door_left_open.png");
		doorSensorLeft.addState("Closed", "false", ImageHelper.IMAGE_FOLDER + "/Sensor/door_closed.png");
		doorSensorLeft.addAllowed(new ValueTypes[] { ValueTypes.SENSOR_MOTION, ValueTypes.SENSOR_BINARY });
		sensorGroup.addView(doorSensorLeft);

		MultiStateButton doorSensorRight = new MultiStateButton("doorSensorRight", ImageHelper.IMAGE_FOLDER + "/Sensor/door_right_open.png", ImageHelper.IMAGE_FOLDER + "/Sensor/door_right_open.png", "Door Sensor (right)");
		doorSensorRight.addState("Open", "true", ImageHelper.IMAGE_FOLDER + "/Sensor/door_right_open.png");
		doorSensorRight.addState("Closed", "false", ImageHelper.IMAGE_FOLDER + "/Sensor/door_closed.png");
		doorSensorRight.addAllowed(new ValueTypes[] { ValueTypes.SENSOR_MOTION, ValueTypes.SENSOR_BINARY });
		sensorGroup.addView(doorSensorRight);

		returnList.add(sensorGroup);

		/* Heating */
		ViewGroup heatingGroup = new ViewGroup();
		heatingGroup.setTitle("Heating");

		MultiStateButton heatingButton = new MultiStateButton("heatingButton", ImageHelper.IMAGE_FOLDER + "/Heating/heating_normal.png", ImageHelper.IMAGE_FOLDER + "/Heating/heating_normal.png", "");
		heatingButton.addState("Cold", "5", ImageHelper.IMAGE_FOLDER + "/Heating/heating_cold.png");
		heatingButton.addState("Normal", "21", ImageHelper.IMAGE_FOLDER + "/Heating/heating_normal.png");
		heatingButton.addState("Hot", "25", ImageHelper.IMAGE_FOLDER + "/Heating/heating_hot.png");
		heatingButton.addAllowed(new ValueTypes[] { ValueTypes.HEATING });
		heatingGroup.addView(heatingButton);

		MultiStateButton awayHome = new MultiStateButton("awayHome", ImageHelper.IMAGE_FOLDER + "/Heating/Not_home.png", ImageHelper.IMAGE_FOLDER + "/Heating/Not_home.png", "Home/Not Home");
		awayHome.addState("On", "true", ImageHelper.IMAGE_FOLDER + "/Heating/Not_home.png");
		awayHome.addState("Off", "false", ImageHelper.IMAGE_FOLDER + "/Heating/Home.png");
		awayHome.addAllowed(new ValueTypes[] { ValueTypes.SENSOR_MOTION, ValueTypes.SENSOR_BINARY, ValueTypes.SWITCH_BINARY });
		heatingGroup.addView(awayHome);

		heatingGroup.addView(new PlusMinValue("plusMinValue", ImageHelper.IMAGE_FOLDER + "/Sensor/PlusMinValue.png", ImageHelper.IMAGE_FOLDER + "/Sensor/PlusMinValue_wide.png", "Plus/Min Value", 0.5, "#ffffff", 50).addAllowed(new ValueTypes[] { ValueTypes.HEATING, ValueTypes.DIMMER, ValueTypes.SENSOR_TEMPERATURE }));

		returnList.add(heatingGroup);
		/* Camera */
		ViewGroup cameraGroup = new ViewGroup();
		cameraGroup.setTitle("Camera");
		cameraGroup.addView(new Camera("cameraPlaceholder", ImageHelper.IMAGE_FOLDER + "/Camera/camera.png", ImageHelper.IMAGE_FOLDER + "/Camera/camera.png", "", 600, false, false).addAllowed(new ValueTypes[] { ValueTypes.IP_CAMERA }));
		returnList.add(cameraGroup);

		/* Clocks */
		ViewGroup clocksGroup = new ViewGroup();
		clocksGroup.setTitle("Clock");
		clocksGroup.addView(new ClockAnalog("analogClock", ImageHelper.IMAGE_FOLDER + "/Clock/analog_clock_icon.png", ImageHelper.IMAGE_FOLDER + "/Clock/analog_clock_icon.png", "Analog Clock").addAllowed(new ValueTypes[] {}));
		clocksGroup.addView(new ClockDigital("digitalClock", ImageHelper.IMAGE_FOLDER + "/Clock/digital_clock_icon.png", ImageHelper.IMAGE_FOLDER + "/Clock/digital_clock.png", "Digital Clock", "#ffffff", 50.0, false).addAllowed(new ValueTypes[] {}));

		returnList.add(clocksGroup);

		/* Shapes & text */
		ViewGroup shapesGroup = new ViewGroup();
		shapesGroup.setTitle("Shapes");
		shapesGroup.addView(new Shape("rectangleShape", ImageHelper.IMAGE_FOLDER + "/Shapes/rectangle_icon.png", ImageHelper.IMAGE_FOLDER + "/Shapes/rectangle_icon.png", "Rectangle", "#c1d4a5", 0.0, false).addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION, ValueTypes.SCENE_ACTIVATION, ValueTypes.PAGE_NAVIGATION }));
		shapesGroup.addView(new Shape("roundedRectangleShape", ImageHelper.IMAGE_FOLDER + "/Shapes/rounded_rectangle_icon.png", ImageHelper.IMAGE_FOLDER + "/Shapes/rounded_rectangle_icon.png", "Rounded Rectangle", "#7dabff", 20.0, false).addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION, ValueTypes.SCENE_ACTIVATION, ValueTypes.PAGE_NAVIGATION }));
		shapesGroup.addView(new Shape("circleShape", ImageHelper.IMAGE_FOLDER + "/Shapes/circle_icon.png", ImageHelper.IMAGE_FOLDER + "/Shapes/circle_icon.png", "Circle", "#ff5757", 250.0, false).addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION, ValueTypes.SCENE_ACTIVATION, ValueTypes.PAGE_NAVIGATION }));
		returnList.add(shapesGroup);

		/* Text */
		ViewGroup textGroup = new ViewGroup();
		textGroup.setTitle("Text & Web Content");
		textGroup.addView(new Text("textContent", ImageHelper.IMAGE_FOLDER + "/Shapes/text_content_icon.png", ImageHelper.IMAGE_FOLDER + "/Shapes/text_content_icon.png", "Text Content", "#ffffff", 55.0, "Text content", Alignments.LEFT).addAllowed(new ValueTypes[] { ValueTypes.TEXT }));
		textGroup.addView(new WebStaticHtml("staticHtmlContent", ImageHelper.IMAGE_FOLDER + "/Shapes/web_static_html.png", ImageHelper.IMAGE_FOLDER + "/Shapes/web_static_html.png", "HTML Content", "<div style='background-color: white;width:100%;height:100%'><h1>Title</h1><strong>Bold text</strong> <u>Underline</u></div>").addAllowed(new ValueTypes[] { ValueTypes.WEB_STATIC_HTML }));
		textGroup.addView(new WebLink("linkWebContent", ImageHelper.IMAGE_FOLDER + "/Shapes/web_link.png", ImageHelper.IMAGE_FOLDER + "/Shapes/web_link.png", "Web Link", "http://deredactie.be").addAllowed(new ValueTypes[] {}));
		textGroup.addView(new WebRSS("linkWebRSS", ImageHelper.IMAGE_FOLDER + "/Shapes/web_rss.png", ImageHelper.IMAGE_FOLDER + "/Shapes/web_rss.png", "RSS Feed", "http://deredactie.be/cm/vrtnieuws?mode=atom", "#ffffff").addAllowed(new ValueTypes[] {}));

		returnList.add(textGroup);
		/* MusicControls */
		ViewGroup musicPlayerGroup = new ViewGroup();
		musicPlayerGroup.setTitle("Music Player");
		musicPlayerGroup.addView(new TrackDisplay("trackDisplay", ImageHelper.IMAGE_FOLDER + "/MusicControls/artist_title.png", ImageHelper.IMAGE_FOLDER + "/MusicControls/artist_title.png", "", "#ffffff", 40.0).addAllowed(new ValueTypes[] { ValueTypes.MUSIC_TRACK_DISPLAY }));
		musicPlayerGroup.addView(new Playlist("playlist", ImageHelper.IMAGE_FOLDER + "/MusicControls/playlist.png", ImageHelper.IMAGE_FOLDER + "/MusicControls/playlist.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_PLAYLIST }));
		musicPlayerGroup.addView(new AlbumImage("albumImage", ImageHelper.IMAGE_FOLDER + "/MusicControls/album.png", ImageHelper.IMAGE_FOLDER + "/MusicControls/album.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ALBUM_IMAGE }));
		musicPlayerGroup.addView(new TrackProgress("trackProgress", ImageHelper.IMAGE_FOLDER + "/Light/slider_icon.png", ImageHelper.IMAGE_FOLDER + "/Light/slider.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_PROGRESS }));
		// musicPlayerGroup.addView(new
		// ImageButton("audioTitleIcon",IMAGE_FOLDER +
		// "/MusicControls/audo_title_icon.png", IMAGE_FOLDER +
		// "/MusicControls/audo_title.png", "", USER_IMAGES +
		// "/MusicControls/audo_title.png").addAllowed(new ValueTypes[] {
		// ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND,
		// ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION }));
		musicPlayerGroup.addView(new Playlists("ejectIcon", ImageHelper.IMAGE_FOLDER + "/MusicControls/eject.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_PLAYLISTS }));
		MultiStateButton playPauseButton = new MultiStateButton("playPauseButton", ImageHelper.IMAGE_FOLDER + "/MusicControls/play.png", ImageHelper.IMAGE_FOLDER + "/MusicControls/play.png", "");
		playPauseButton.addState("Paused", "PAUSE", ImageHelper.IMAGE_FOLDER + "/MusicControls/play.png");
		playPauseButton.addState("Playing", "PLAY", ImageHelper.IMAGE_FOLDER + "/MusicControls/pause.png");
		playPauseButton.addAllowed(new ValueTypes[] { ValueTypes.MUSIC_PLAY_PAUSE });
		musicPlayerGroup.addView(playPauseButton);
		MultiStateButton randomButton = new MultiStateButton("randomButton", ImageHelper.IMAGE_FOLDER + "/MusicControls/random_off.png", ImageHelper.IMAGE_FOLDER + "/MusicControls/random_off.png", "");
		randomButton.addState("Off", "false", ImageHelper.IMAGE_FOLDER + "/MusicControls/random_off.png");
		randomButton.addState("On", "true", ImageHelper.IMAGE_FOLDER + "/MusicControls/random_on.png");
		randomButton.addAllowed(new ValueTypes[] { ValueTypes.MUSIC_RANDOM });
		musicPlayerGroup.addView(randomButton);
		musicPlayerGroup.addView(new ImageButton("backwardButton", ImageHelper.IMAGE_FOLDER + "/MusicControls/skip_backward.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION }));
		musicPlayerGroup.addView(new ImageButton("forwardButton", ImageHelper.IMAGE_FOLDER + "/MusicControls/skip_forward.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION }));
		musicPlayerGroup.addView(new ImageButton("stopButton", ImageHelper.IMAGE_FOLDER + "/MusicControls/stop.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION }));
		musicPlayerGroup.addView(new ImageButton("volumeDownButton", ImageHelper.IMAGE_FOLDER + "/MusicControls/volume_down.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION }));
		musicPlayerGroup.addView(new ImageButton("volumeUpButton", ImageHelper.IMAGE_FOLDER + "/MusicControls/volume_up.png", "").addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION }));
		returnList.add(musicPlayerGroup);

		/* User images */
		File userImagesFolder = new File(SettingsManager.getBasePath() + Server.FILESERVER_PATH + ImageHelper.USER_IMAGES);
		if (!userImagesFolder.exists()) {
			userImagesFolder.mkdirs();
		}
		ViewGroup userImages = this.getImagesIn(userImagesFolder.getAbsolutePath());
		userImages.setTitle("User");
		returnList.add(userImages);

		return returnList;
	}

	public JSONArray getImagePathJSON(String filePathString) {
		try {
			// Add all images in folder HomeDesigner/images/User/backgrounds to
			// the json file
			File imageFolder = new File(SettingsManager.getBasePath() + Server.FILESERVER_PATH + ImageHelper.USER_IMAGES + "/" + filePathString);
			File[] filePaths = imageFolder.listFiles();
			JSONArray returnArray = new JSONArray();
			JSONObject userBackgrounds = new JSONObject();
			userBackgrounds.put("title", "Backgrounds");
			JSONArray backgroundArray = new JSONArray();
			if (filePaths != null) {
				for (File filePath : filePaths) {
					String extension = Util.getExtension(filePath);
					if (filePath.isFile() && (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("gif") || extension.equals("bmp"))) {
						JSONObject newImageObject = new JSONObject();
						newImageObject.put("type", "image");
						newImageObject.put("id", "user");
						newImageObject.put("icon", ImageHelper.USER_IMAGES + "/" + filePathString + "/" + filePath.getName());
						newImageObject.put("image", ImageHelper.USER_IMAGES + "/" + filePathString + "/" + filePath.getName());
						backgroundArray.put(newImageObject);
					}
				}
			}
			userBackgrounds.put("objects", backgroundArray);
			returnArray.put(userBackgrounds);
			return returnArray;
		} catch (JSONException e) {
			ImageHelper.log.error("Exception occured: ", e);
		}
		return null;
	}

	// Read JSON-file api/static/images.json
	/*
	 * public JSONArray getImagesJSON() {
	 * 
	 * File imageJSONFile = new File(Settings.getBasePath() +
	 * NetWebSocketServer.FILESERVER_PATH + STATIC_API + STATIC_IMAGE_FILE);
	 * if(imageJSONFile.exists()) { FileInputStream inputStream = null; try {
	 * inputStream = new FileInputStream(Settings.getBasePath() +
	 * NetWebSocketServer.FILESERVER_PATH + STATIC_API + STATIC_IMAGE_FILE);
	 * 
	 * String staticJSONImageContent = IOUtils.toString(inputStream);
	 * 
	 * imageArray = new JSONArray(staticJSONImageContent);
	 * 
	 * int i = 0; JSONArray userImages = null; while (userImages == null && i <
	 * imageArray.length()) { JSONObject tmp = imageArray.getJSONObject(i); if
	 * (tmp.getString("title").equals("User")) { userImages =
	 * tmp.getJSONArray("objects") ; } i++; }
	 * 
	 * // Add all images in folder baseFolderUserImages to the json file File
	 * imageFolder = new File(Settings.getBasePath() +
	 * NetWebSocketServer.FILESERVER_PATH + HOMEDESIGNER_PATH + IMAGE_FOLDER +
	 * USER_IMAGES); File[] filePaths = imageFolder.listFiles();
	 * 
	 * for (File filePath : filePaths) { String extension =
	 * Util.getExtension(filePath); if(filePath.isFile() && (
	 * extension.equals("jpg") || extension.equals("jpeg") ||
	 * extension.equals("png") || extension.equals("gif") ||
	 * extension.equals("bmp") )) { JSONObject newImageObject = new
	 * JSONObject(); newImageObject.put("type", "image");
	 * newImageObject.put("id", "user"); newImageObject.put("icon",
	 * IMAGE_FOLDER.substring(1) + USER_IMAGES + "/" + filePath.getName());
	 * newImageObject.put("image", IMAGE_FOLDER.substring(1) + USER_IMAGES + "/"
	 * + filePath.getName()); userImages.put(newImageObject); } }
	 * 
	 * 
	 * } catch (JSONException e) { log.error("Exception occured: ",e); } catch
	 * (FileNotFoundException e1) { e1.printStackTrace(); } catch (IOException
	 * e) { log.error("[API] Static image file cannot be read"); } finally { try
	 * { inputStream.close(); } catch (IOException e) { } } }else { log.error(
	 * "[API] Static image file does not extist"); }
	 * 
	 * return imageArray; }
	 */

	public ViewGroup getImagesIn(String filePathString) {
		ViewGroup resultGroup = new ViewGroup();

		File imageFolder = new File(filePathString);
		File[] filePaths = imageFolder.listFiles();

		if (filePaths != null) {
			for (File filePath : filePaths) {
				String extension = Util.getExtension(filePath);
				if (filePath.isFile() && (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("gif") || extension.equals("bmp"))) {
					String type = ViewTypes.IMAGE_BUTTON.convert();
					String icon = ImageHelper.IMAGE_FOLDER + "/" + imageFolder.getName() + "/" + filePath.getName();
					String id = Util.MD5(type + icon + icon + "");
					ImageButton imageButton = new ImageButton(id, icon, "");
					imageButton.addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION, ValueTypes.SCENE_ACTIVATION, ValueTypes.PAGE_NAVIGATION });
					resultGroup.addView(imageButton);
				}
			}
		}

		return resultGroup;
	}

	/*
	 * public JSONObject getImage(String imageID) { boolean found = false; int i
	 * = 0; int j=0;
	 * 
	 * if(imageArray == null) { imageArray = this.getImagesJSON(); }
	 * 
	 * while(!found & i<imageArray.length()) { JSONObject imageTray; try {
	 * imageTray = imageArray.getJSONObject(i);
	 * 
	 * JSONArray objectsArray = imageTray.getJSONArray("objects"); while(!found
	 * && j < objectsArray.length()) { JSONObject imageObjects =
	 * objectsArray.getJSONObject(j); String imageNr = ""; try { imageNr =
	 * imageObjects.getInt("id")+""; } catch (JSONException e) { // User images
	 * (nr 'user') imageNr = imageObjects.getString("id")+""; }
	 * if(imageNr.equals(imageID)) { found = true; return imageObjects; } j++; }
	 * } catch (JSONException e) { log.error("Exception occured: ",e); } i++; j
	 * = 0; }
	 * 
	 * return null; }
	 */
}
