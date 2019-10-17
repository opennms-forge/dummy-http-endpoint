/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019-2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.demo.webhook;

import static org.springframework.http.MediaTypeFactory.getMediaType;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/files")
public class FileUploadRestController {

    @Value("${files.upload.dir}")
    private String uploadDir;

    @PostConstruct
    public void init() throws IOException {
        if (!Files.exists(Paths.get(uploadDir))) {
            Files.createDirectories(Paths.get(uploadDir));
        }
    }

    @GetMapping(params = "{id}")
    public ResponseEntity<Resource> getFile(@PathVariable String instanceId) {
        final FileSystemResource resource = new FileSystemResource(Paths.get(uploadDir, instanceId));
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        final Optional<MediaType> mediaTypeOptional = getMediaType(resource);
        final MediaType mediaType = mediaTypeOptional.orElseThrow(() -> new RuntimeException("Unknown media type. Bailing"));
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping
    public ResponseEntity<String> listFiles() throws IOException {
        final JSONArray objects = new JSONArray();
        final List<JSONObject> files = Files.list(Paths.get(uploadDir))
                .filter(p -> Files.isReadable(p) && p.toString().endsWith(".pdf") || p.toString().endsWith(".csv"))
                .map(p -> {
                    final JSONObject file = new JSONObject();
                    file.put("name", p.getFileName().toString());
                    file.put("extension", StringUtils.getFilenameExtension(p.getFileName().toString()));
                    return file;
                })
                .collect(Collectors.toList());
        objects.put(files);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objects.toString());
    }

    @RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT }, consumes = { "multipart/form-data" })
    ResponseEntity<?> saveFile(@RequestParam("file") MultipartFile file, @RequestParam("instanceId") String instanceId) throws Exception {
        if (instanceId == null) {
            return ResponseEntity.badRequest().build();
        }
        try (InputStream in = file.getInputStream();
             OutputStream out = new FileOutputStream(Paths.get(uploadDir, file.getOriginalFilename()).toFile()))
        {
            FileCopyUtils.copy(in, out);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        final URI location = fromCurrentRequest().buildAndExpand(instanceId).toUri();
        return ResponseEntity.created(location).build();
    }
}
