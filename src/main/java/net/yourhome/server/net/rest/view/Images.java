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

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.base.enums.ViewTypes;
import net.yourhome.common.net.model.viewproperties.ImageButton;
import net.yourhome.common.net.model.viewproperties.ViewGroup;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;
import net.yourhome.server.net.Server;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.log4j.Logger;
import org.json.JSONArray;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
				ViewGroup folderImages = this.imageHelper.getImagesIn(subFolder.getAbsolutePath());
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
		JSONArray imageArray = this.imageHelper.getImagePathJSON(imagePath);

		// Build return message
		return imageArray.toString();
	}

	// POST /api/Images/StageImage
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("StageImage")
	@POST
	public ImageButton uploadStageImage(final MultipartBody multipart) {
		String path = this.uploadImage(multipart, SettingsManager.getBasePath() + Server.FILESERVER_PATH, ImageHelper.USER_IMAGES + "/");
		String type = ViewTypes.IMAGE_BUTTON.convert();
		String id = Util.MD5(type + path + path + "");
		ImageButton imageButton = new ImageButton(id, path, "");
		imageButton.addAllowed(new ValueTypes[] { ValueTypes.MUSIC_ACTION, ValueTypes.GENERAL_COMMAND, ValueTypes.HTTP_COMMAND, ValueTypes.RADIO_STATION, ValueTypes.SCENE_ACTIVATION, ValueTypes.PAGE_NAVIGATION });
		return imageButton;
	}

	// POST /api/Images/backgroundImageSrc
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("backgroundImageSrc")
	@POST
	public String uploadbackgroundImageSrc(final MultipartBody multipart) {
		String path = this.uploadImage(multipart, SettingsManager.getBasePath() + Server.FILESERVER_PATH, ImageHelper.USER_BACKGROUND_IMAGES + "/");
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
			Images.log.error("[API] Cannot read uploaded file");
			return "";
		}
	}

}
