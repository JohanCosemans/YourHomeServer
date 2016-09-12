package net.yourhome.server.net.rest.view;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.json.JSONArray;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.base.enums.ViewTypes;
import net.yourhome.common.net.model.viewproperties.ImageButton;
import net.yourhome.common.net.model.viewproperties.ViewGroup;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;
import net.yourhome.server.net.Server;

@Path("/Images")
public class Images {

	private static Logger log = Logger.getLogger(Images.class);

	public static final int MAX_SIZE_IN_MB = 10;

	private ImageHelper imageHelper = ImageHelper.getInstance();

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	// GET /api/Images
	public String get() {
		List<ViewGroup> allImagesInViewElements = new ArrayList<ViewGroup>();
		// Read all folders and create images for them
		File imagesFolder = new File(SettingsManager.getBasePath() + Server.FILESERVER_PATH + ImageHelper.IMAGE_FOLDER);
		for (File subFolder : imagesFolder.listFiles()) {
			if (subFolder.isDirectory()) {
				ViewGroup folderImages = imageHelper.getImagesIn(subFolder.getAbsolutePath());
				if (folderImages.getObjects().size() > 0) {
					folderImages.setTitle(subFolder.getName());
					allImagesInViewElements.add(folderImages);
				}
			}
		}
		// Build return message
		return new JSONArray(allImagesInViewElements).toString();
	}

	/*
	 * @GET
	 * 
	 * @Produces( { MediaType.APPLICATION_JSON } )
	 * 
	 * @Path( "backgrounds" ) // GET /api/Images/backgrounds public String
	 * getBackgrounds() { // Get images array JSONArray imageArray =
	 * imageHelper.getImagePathJSON(ImageHelper.USER_BACKGROUND_IMAGES);
	 * 
	 * // Build return message return imageArray.toString(); }
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/{imagePath}")
	// GET /api/Images/backgrounds
	public String getBackgrounds(@PathParam("imagePath") final String imagePath) {
		// Get images array
		JSONArray imageArray = imageHelper.getImagePathJSON(imagePath);

		// Build return message
		return imageArray.toString();
	}

	// POST /api/Images/StageImage
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("StageImage")
	@POST
	public ImageButton uploadStageImage(final MultipartBody multipart) {
		String path = uploadImage(multipart, SettingsManager.getBasePath() + Server.FILESERVER_PATH, ImageHelper.USER_IMAGES + "/");
		String type = ViewTypes.IMAGE_BUTTON.convert();
		String id = Util.MD5(type+path+path+"");
		ImageButton imageButton = new ImageButton(id, path, "");
		imageButton.addAllowed(new ValueTypes[] { ValueTypes.SCENE_ACTIVATION, ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION });
		return imageButton;
	}

	// POST /api/Images/backgroundImageSrc
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("backgroundImageSrc")
	@POST
	public String uploadbackgroundImageSrc(final MultipartBody multipart) {
		String path = uploadImage(multipart, SettingsManager.getBasePath() + Server.FILESERVER_PATH, ImageHelper.USER_BACKGROUND_IMAGES + "/");
		return "{ \"path\" : \"" + path + "\" }";
	}

	public String uploadImage(final MultipartBody multipart, String folderPath, String webPath) {
		Attachment attachment = multipart.getRootAttachment();
		String imageName = attachment.getContentDisposition().getParameter("filename");
		if (imageName == null || imageName.equals("")) {
			imageName = UUID.randomUUID().toString();
		}

		InputStream in;
		try {
			in = attachment.getDataHandler().getInputStream();

			// Copy the file to its location.
			Util.writeToFile(in, new File(folderPath + webPath + imageName));

			return webPath + imageName;

		} catch (IOException e1) {
			log.error("[API] Cannot read uploaded file");
			return "";
		}
	}

}
