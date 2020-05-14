package eu.europa.ec.itb.json.validation;

import eu.europa.ec.itb.json.ApplicationConfig;
import eu.europa.ec.itb.json.DomainConfig;
import eu.europa.ec.itb.json.DomainConfigCache;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class FileManager {

	private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	@Autowired
	private ApplicationConfig config = null;

	@Autowired
	private DomainConfigCache domainConfigCache = null;

	private ConcurrentHashMap<String, ReadWriteLock> externalDomainFileCacheLocks = new ConcurrentHashMap<>();

	private File getURLFile(String targetFolder, String URLConvert) throws IOException {
		URL url = new URL(URLConvert);
		String extension = FilenameUtils.getExtension(url.getFile());
		return getURLFile(targetFolder, URLConvert, extension);
	}

	private File getURLFile(String targetFolder, String URLConvert, String extension) throws IOException {
		Path tmpPath;
		if (extension == null) {
			extension = ".json";
		} else if (!extension.startsWith(".")) {
			extension = "." + extension;
		}
		tmpPath = getFilePath(targetFolder, extension);
		try (InputStream in = getURIInputStream(URLConvert)){
			Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
		}
		return tmpPath.toFile();
	}

	private InputStream getURIInputStream(String URLConvert) {
        // Read the string from the provided URI.
        URI uri = URI.create(URLConvert);
        Proxy proxy = null;
        List<Proxy> proxies = ProxySelector.getDefault().select(uri);
        if (proxies != null && !proxies.isEmpty()) {
            proxy = proxies.get(0);
        }
        
        try {
	        URLConnection connection;
	        if (proxy == null) {
	            connection = uri.toURL().openConnection();
	        } else {
	            connection = uri.toURL().openConnection(proxy);
	        }
	        
	        return connection.getInputStream();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read provided URI", e);
        }
        
	}

	public File getStringFile(File targetFolder, String content) throws IOException {
		return getStringFile(targetFolder.getAbsolutePath(), content);
	}

	private File getStringFile(String targetFolder, String content) throws IOException {
		String extension = ".json";
		Path tmpPath;
		tmpPath = getFilePath(targetFolder, extension);
		try (InputStream in = new ByteArrayInputStream(content.getBytes())){
			Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
		}
		return tmpPath.toFile();
	}

	public File getURLFile(File targetFolder, String URLConvert) throws IOException {
		return getURLFile(targetFolder.getAbsolutePath(), URLConvert);
	}

	public List<FileInfo> getRemoteExternalSchemas(File parentFolder, List<FileContent> externalSchemas) throws IOException {
		List<FileInfo> externalSchemaFiles = new ArrayList<>();
		if (externalSchemas != null && !externalSchemas.isEmpty()) {
			File externalSchemaFolder = new File(parentFolder, UUID.randomUUID().toString());
			for (FileContent externalSchema: externalSchemas) {
				File contentFile;
				if (externalSchema.getEmbeddingMethod() != null) {
					switch(externalSchema.getEmbeddingMethod()) {
						case FileContent.embedding_URL:
							try{
								contentFile = getURLFile(externalSchemaFolder, externalSchema.getContent());
							} catch(IOException e) {
								throw new IllegalArgumentException("Error when transforming the URL into File.", e);
							}
							break;
						case FileContent.embedding_BASE64:
							contentFile = getBase64File(externalSchemaFolder, externalSchema.getContent());
							break;
						case FileContent.embedding_STRING:
							try{
								contentFile = getStringFile(externalSchemaFolder, externalSchema.getContent());
							}catch(IOException e) {
								throw new IllegalArgumentException("Error when transforming the STRING into File.", e);
							}
							break;
						default:
							throw new IllegalArgumentException("Unexpected embedding method ["+externalSchema.getEmbeddingMethod()+"]");
					}
				} else {
					contentFile = getFileAsUrlOrBase64(externalSchemaFolder, externalSchema.getContent());
				}
				externalSchemaFiles.add(new FileInfo(contentFile));
			}
		}
		return externalSchemaFiles;
	}

	public File createTemporaryFolderPath() {
		return createTemporaryFolderPath(new File(config.getTmpFolder()));
	}

	private File createTemporaryFolderPath(File parentFolder) {
		UUID folderUUID = UUID.randomUUID();
		Path tmpFolder = Paths.get(parentFolder.getAbsolutePath(), folderUUID.toString());
		return tmpFolder.toFile();
	}

	private Path getFilePath(String folder, String extension) {
		Path tmpPath = Paths.get(folder, UUID.randomUUID().toString() + extension);
		tmpPath.toFile().getParentFile().mkdirs();

		return tmpPath;
	}

    private File getTempFolder() {
    	return new File(config.getTmpFolder());
	}

    private File getRemoteFileCacheFolder() {
    	return new File(getTempFolder(), "remote_config");
	}

	public File getBase64File(File targetFolder, String base64Convert) {
		if (targetFolder == null) {
			targetFolder = getTempFolder();
		}
		File tempFile;
		try {
			tempFile = getFilePath(targetFolder.getAbsolutePath(), "").toFile();
			// Construct the string from its BASE64 encoded bytes.
			byte[] decodedBytes = Base64.getDecoder().decode(base64Convert);
			FileUtils.writeByteArrayToFile(tempFile, decodedBytes);
		} catch (IOException e) {
			throw new IllegalStateException("Error when transforming the Base64 into File.", e);
		}
		return tempFile;
	}

	public File getFileAsUrlOrBase64(File targetFolder, String content) throws IOException {
		if (targetFolder == null) {
			targetFolder = getTempFolder();
		}
		File outputFile;
		try {
			outputFile = getURLFile(targetFolder, content);
		} catch(MalformedURLException e) {
			outputFile = getBase64File(targetFolder, content);
		}
		return outputFile;
	}

	@PostConstruct
	public void init() {
		FileUtils.deleteQuietly(getTempFolder());
		for (DomainConfig config: domainConfigCache.getAllDomainConfigurations()) {
			externalDomainFileCacheLocks.put(config.getDomainName(), new ReentrantReadWriteLock());
		}
		FileUtils.deleteQuietly(getRemoteFileCacheFolder());
		resetRemoteFileCache();
	}

	List<FileInfo> getPreconfiguredSchemaFileInfos(DomainConfig domainConfig, String validationType){
		List<FileInfo> shaclFiles = new ArrayList<>();
		for (File localFile: getLocalSchemaFiles(domainConfig, validationType)) {
			shaclFiles.addAll(getLocalSchemaFileInfos(localFile));
		}
		shaclFiles.addAll(getRemoteSchemaFileInfos(domainConfig, validationType));
		return shaclFiles;
	}

	/**
	 * Return the schema files loaded for a given validation type
	 * @return File
	 */
	private List<File> getLocalSchemaFiles(DomainConfig domainConfig, String validationType) {
		List<File> localFileReferences = new ArrayList<>();
		String localFolderConfigValue = domainConfig.getSchemaFile().get(validationType).getLocalFolder();
		if (StringUtils.isNotEmpty(localFolderConfigValue)) {
			String[] localFiles = StringUtils.split(localFolderConfigValue, ',');
			for (String localFile: localFiles) {
				localFileReferences.add(Paths.get(config.getResourceRoot(), domainConfig.getDomain(), localFile.trim()).toFile());
			}
		}
		return localFileReferences;
	}

	/**
	 * Returns the list of files (if it is a directory) or the file and the corresponding content type.
	 * @return The file information
	 */
	private List<FileInfo> getLocalSchemaFileInfos(File schemaFileOrFolder){
		List<FileInfo> fileInfo = new ArrayList<>();
		if (schemaFileOrFolder != null && schemaFileOrFolder.exists()) {
			if (schemaFileOrFolder.isFile()) {
				// We are pointing to a single file.
				fileInfo.add(new FileInfo(schemaFileOrFolder));
			} else {
				// Get all files.
				File[] files = schemaFileOrFolder.listFiles();
				if (files != null) {
					for (File aSchemaFile: files) {
						if (aSchemaFile.isFile()) {
							if (config.getAcceptedSchemaExtensions().contains(FilenameUtils.getExtension(aSchemaFile.getName().toLowerCase()))) {
								fileInfo.add(new FileInfo(aSchemaFile));
							}
						}
					}
				}
			}
		}

		return fileInfo;
	}

	private List<FileInfo> getRemoteSchemaFileInfos(DomainConfig domainConfig, String validationType) {
		File remoteConfigFolder = new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType);
		if (remoteConfigFolder.exists()) {
			return getLocalSchemaFileInfos(remoteConfigFolder);
		} else {
			return Collections.emptyList();
		}
	}

	@Scheduled(fixedDelayString = "${validator.cleanupRate}")
	private void resetRemoteFileCache() {
		logger.debug("Resetting remote file cache");
		for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
			try {
				// Get write lock for domain.
				logger.debug("Waiting for lock to reset cache for ["+domainConfig.getDomainName()+"]");
				externalDomainFileCacheLocks.get(domainConfig.getDomainName()).writeLock().lock();
				logger.debug("Locked cache for ["+domainConfig.getDomainName()+"]");
				for (String validationType: domainConfig.getType()) {
					// Empty cache folder.
					File remoteConfigFolder = new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType);
					FileUtils.deleteQuietly(remoteConfigFolder);
					remoteConfigFolder.mkdirs();
					// Download remote schema files (if needed).
					List<DomainConfig.RemoteInfo> ri = domainConfig.getSchemaFile().get(validationType).getRemote();
					if (ri != null) {
						try {
							for (DomainConfig.RemoteInfo info: ri) {
								getURLFile(remoteConfigFolder.getAbsolutePath(), info.getUrl(), null);
							}
						} catch (IOException e) {
							logger.error("Error to load the remote schema file", e);
							throw new IllegalStateException("Error to load the remote schema file", e);
						}
					}
				}
			} finally {
				// Unlock domain.
				externalDomainFileCacheLocks.get(domainConfig.getDomainName()).writeLock().unlock();
				logger.debug("Reset remote schema file cache for ["+domainConfig.getDomainName()+"]");
			}
		}
	}

	void signalValidationStart(String domainName) {
		logger.debug("Signalling validation start for ["+domainName+"]");
		externalDomainFileCacheLocks.get(domainName).readLock().lock();
		logger.debug("Signalled validation start for ["+domainName+"]");
	}

	void signalValidationEnd(String domainName) {
		logger.debug("Signalling validation end for ["+domainName+"]");
		externalDomainFileCacheLocks.get(domainName).readLock().unlock();
		logger.debug("Signalled validation end for ["+domainName+"]");
	}

}
