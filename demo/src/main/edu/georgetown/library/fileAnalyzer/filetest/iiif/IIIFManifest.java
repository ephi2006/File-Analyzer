package edu.georgetown.library.fileAnalyzer.filetest.iiif;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;

import org.apache.tika.exception.TikaException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import edu.georgetown.library.fileAnalyzer.filetest.iiif.IIIFEnums.DefaultDimensions;
import edu.georgetown.library.fileAnalyzer.filetest.iiif.IIIFEnums.IIIFArray;
import edu.georgetown.library.fileAnalyzer.filetest.iiif.IIIFEnums.IIIFLookup;
import edu.georgetown.library.fileAnalyzer.filetest.iiif.IIIFEnums.IIIFProp;
import edu.georgetown.library.fileAnalyzer.filetest.iiif.IIIFEnums.IIIFType;
import edu.georgetown.library.fileAnalyzer.util.XMLUtil;

public class IIIFManifest {
        private File file;
        protected JSONObject jsonObject;
        protected String iiifRootPath;
        protected JSONObject seq;
        protected XPath xp;
        
        protected HashMap<File,JSONObject> ranges = new HashMap<>();
        
        public static final String EMPTY = "";
        
        
        
        JSONObject top;
        protected MetadataInputFile inputMetadata;

        public void setProperty(JSONObject json, IIIFProp prop) {
                json.put(prop.getLabel(), prop.getDefault());
        }
        public void setProperty(JSONObject json, IIIFProp prop, String value) {
                if (value.equals(EMPTY)) {
                        return;
                }
                if (prop.isMetadata) {
                        addMetadata(json, prop.getLabel(), value);
                } else {
                        json.put(prop.getLabel(), value);
                }
        }
        public void setProperty(JSONObject json, IIIFType type) {
                json.put(IIIFProp.type.getLabel(), type.getValue());
        }

        public JSONArray addArray(JSONObject obj, IIIFArray iiifarr) {
                String arrlabel = iiifarr.getLabel();
                JSONArray arr = null;
                if (obj.has(arrlabel)) {
                        arr = obj.getJSONArray(arrlabel);
                } else {
                        arr = new JSONArray();
                        obj.put(arrlabel, arr);
                }
                return arr;
        }

        public void addMetadata(JSONObject json, String label, String value) {
                JSONArray metadata = addArray(json, IIIFArray.metadata);
                Map<String,String> m = new HashMap<>();
                m.put(IIIFProp.label.name(), label);
                m.put(IIIFProp.value.name(), value);
                metadata.put(m);
        }
       
        public IIIFManifest(MetadataInputFile inputMetadata, String iiifRootPath, File manifestFile, boolean isCollectionManifest) throws IOException {
                checkManifestFile(manifestFile);
                file = manifestFile;
                jsonObject = new JSONObject();
                this.iiifRootPath = iiifRootPath;
                this.inputMetadata = inputMetadata;
                xp = XMLUtil.xf.newXPath();
                
                setProperty(jsonObject, IIIFProp.context);
                setProperty(jsonObject, IIIFType.typeManifest);
                //TODO - make param
                setProperty(jsonObject, IIIFProp.label, inputMetadata.getValue(IIIFLookup.Title, EMPTY)); 
                setProperty(jsonObject, IIIFProp.attribution, inputMetadata.getValue(IIIFLookup.Attribution, EMPTY));

                top = makeRangeObject("Finding Aid","id","Document Type").put("viewingHint", "top");
                seq = addSequence(jsonObject);
                setProperty(jsonObject, IIIFProp.id,"https://repository-dev.library.georgetown.edu/xxx");
        }       
        
        public void setLogoUrl(String s) {
                if (!s.equals(EMPTY)) {
                        setProperty(jsonObject, IIIFProp.logo, s);
                }
        }
        
        public void addManifestToCollection(IIIFManifest itemManifest) {
                //TODO
        }
        
        public File getManifestFile() {
                return file;
        }

        public File getComponentManifestFile(File f, String identifier) {
                return new File(file.getParentFile(), identifier);
        }

        public void checkManifestFile(File manFile) throws IOException {
                try(FileWriter fw = new FileWriter(manFile)){
                        fw.write("");
                } catch (IOException e) {
                        throw e;
                }
                if (!manFile.canWrite()) {
                        throw new IOException(String.format("Cannot write to manifest file [%s]", manFile.getName()));
                }
        }
        
        
        public void set2Page() {
                seq.put("viewingHint", "paged");
        }
        
        
        public void addDirLink(File f, IIIFArray iiifarr, String id) {
                File pfile = f.getParentFile();
                if (pfile == null) {
                        return;
                }
                JSONObject parent = ranges.get(pfile);
                if (parent != null) {
                        addArray(parent, iiifarr).put(id);
                } else {
                        //System.err.println(pfile.getAbsolutePath()+" not found");
                }
        }

        public String translateLabel(String label) {
                return label;
        }
        
        public JSONObject makeRange(File dir) {
                return top;
        }
        
        public JSONObject makeRange(File dir, String label, String id, boolean isTop) {
                return top;
        }       

        public JSONObject makeRangeObject(String label, String id, String labelLabel) {
                JSONObject obj = new JSONObject();
                label = translateLabel(label);
                setProperty(obj, IIIFProp.label, label);
                setProperty(obj, IIIFProp.id, id);
                setProperty(obj, IIIFType.typeRange);
                this.addArray(obj, IIIFArray.ranges);
                addArray(jsonObject, IIIFArray.structures).put(obj);
                return obj;
        }       
        
        public void refine()  {
                //No op - meant to be overridden
        }
        public void write() throws IOException {
                refine();
                FileWriter fw = new FileWriter(file);
                jsonObject.write(fw);
                fw.close();
        }

        public String serialize() {
                return jsonObject.toString(2);
        }
        
        public JSONObject addSequence(JSONObject parent) {
                JSONArray arr = addArray(parent, IIIFArray.sequences);
                JSONObject obj = new JSONObject();
                arr.put(obj);
                setProperty(obj, IIIFProp.id, "https://repository-dev.library.georgetown.edu/seq");
                setProperty(obj, IIIFType.typeSequence);
                addArray(obj, IIIFArray.canvases);
                return obj;
        }
        //public JSONObject addFile(File f) {
        //        return addCanvas(f.getName(), f);
        //}
        public JSONObject addFile(String key, File f, MetadataInputFile itemMeta) {
                return addCanvas(key, f, itemMeta);
        }
        public String translateItemLabel(String label) {
                return label;
        }
        
        public String getIIIFPath(String key, File f) {
                return String.format("%s/%s", iiifRootPath, key.replaceAll("\\\\",  "/").replaceFirst("^/*", ""));
        }
       
        public void addCanvasMetadata(JSONObject canvas, File f, MetadataInputFile itemMeta) {
                setProperty(canvas, IIIFProp.label, itemMeta.getValue(IIIFLookup.Title, EMPTY));
                setProperty(canvas, IIIFProp.dateCreated, itemMeta.getValue(IIIFLookup.DateCreated, EMPTY));
                setProperty(canvas, IIIFProp.creator, itemMeta.getValue(IIIFLookup.Creator, EMPTY));
                setProperty(canvas, IIIFProp.description, itemMeta.getValue(IIIFLookup.Description, EMPTY));
                setProperty(canvas, IIIFProp.subject, itemMeta.getValue(IIIFLookup.Subject, EMPTY));
                setProperty(canvas, IIIFProp.rights, itemMeta.getValue(IIIFLookup.Rights, EMPTY));
                setProperty(canvas, IIIFProp.permalink, itemMeta.getValue(IIIFLookup.Permalink, EMPTY));
        }
        
        public void addCanvasToManifest(JSONObject canvas) {
                JSONArray arr = addArray(seq, IIIFArray.canvases);
                arr.put(canvas);
        }
        
        public JSONObject addCanvas(String key, File f, MetadataInputFile itemMeta) {
                String iiifpath = getIIIFPath(key, f);
                String canvasid = "https://repository-dev.library.georgetown.edu/loris/Canvas/"+f.getName();
                String imageid = "https://repository-dev.library.georgetown.edu/loris/Image/"+f.getName();
                String resid = iiifpath + "/full/full/0/default.jpg";
                
                JSONObject canvas = new JSONObject();
                setProperty(canvas, IIIFProp.id, canvasid);
                setProperty(canvas, IIIFType.typeCanvas); 
                ManifestDimensions dim = DefaultDimensions.PORTRAIT.dimensions;
                try {
                        dim = new ManifestDimensions(f);
                } catch (IOException | SAXException | TikaException e) {
                        e.printStackTrace();
                } 
                setProperty(canvas, IIIFProp.height, dim.height());
                setProperty(canvas, IIIFProp.width, dim.width());
                addCanvasMetadata(canvas, f, itemMeta);
                addCanvasToManifest(canvas);
                JSONArray imarr = addArray(canvas, IIIFArray.images);
                JSONObject image = new JSONObject();
                imarr.put(image);
                setProperty(image, IIIFProp.context);
                setProperty(image, IIIFProp.id, imageid); 
                setProperty(image, IIIFType.typeAnnotation);
                image.put("motivation", "sc:painting"); 
                image.put("on", "https://repository-dev.library.georgetown.edu/ead");
                JSONObject resource = new JSONObject();
                image.put("resource", resource);
                setProperty(resource, IIIFProp.id, resid); 
                setProperty(resource, IIIFType.typeImage);
                setProperty(resource, IIIFProp.format, "image/jpeg");
                setProperty(resource, IIIFProp.height, dim.height());
                setProperty(resource, IIIFProp.width, dim.width());                      
                JSONObject service = new JSONObject();
                resource.put("service", service);
                setProperty(service, IIIFProp.context); 
                setProperty(service, IIIFProp.id, iiifpath); 
                setProperty(service, IIIFProp.profile);    
                
                linkCanvas(f, canvasid);
                return canvas;
        }
        
        public void linkCanvas(File f, String canvasid) {
                //no action if canvases only appear in the sequences
        }

        /*
        public void setXPathValue(JSONObject obj, String label, Node d, String xq) {
                try { 
                    obj.put(label, xp.evaluate(xq, d));
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                }
        }

        public String getXPathValue(Node d, String xq, String def) {
                try { 
                    return xp.evaluate(xq, d);
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                }
                return def;
        }
        */
}

/*

public class IIIFManifestAIP extends IIIFManifest {

        protected TreeMap<String,JSONObject> subjranges = new TreeMap<>();
        protected TreeMap<String,JSONObject> datecanvases = new TreeMap<>();
        protected TreeMap<String,JSONObject> dateranges = new TreeMap<>();
        JSONObject allsubjects;
        JSONObject alldates;
        public IIIFManifestAIP(File root, String iiifRootPath, File manifestFile) {
                super(root, iiifRootPath, manifestFile);
                jsonObject.put("label", "Photograph Selections from University Archives");
                jsonObject.put("attribution", "Permission to copy or publish photographs from this collection must be obtained from the Georgetown University Archives.");
                this.addMetadata(jsonObject, METADATA, "Collection", 
                        "Photograph Selections from University Archives");
                SimpleNamespaceContext nsContext = new XMLUtil().new SimpleNamespaceContext();
                nsContext.add("dim", "http://www.dspace.org/xmlns/dspace/dim");
                xp.setNamespaceContext(nsContext);
                alldates = makeRange(root, "Date Ranges","Date Listing", true);
                addArray(top, RANGES).put(alldates.getString("@id"));
                allsubjects = makeRange(root, "All Subjects","Photo Listing", true);
                addArray(top, RANGES).put(allsubjects.getString("@id"));
        }       
        
        public JSONObject getDateRange(String dateCreated) {
                Pattern p = Pattern.compile("^(\\d\\d\\d)\\d.*");
                Matcher m = p.matcher(dateCreated);
                String name = null;
                if (m.matches()) {
                        int year = Integer.parseInt(m.group(1));
                        name = String.format("%d0 - %d0", year, year+1);
                } else {
                        name = "Unspecified";
                }
                JSONObject range = dateranges.get(name);
                if (range == null) {
                        String rangeid = "date-" + name.replaceAll(" ", "");
                        range = makeRangeObject(name, rangeid, "Date Range");
                        dateranges.put(name, range);
                }
                return range;
        }
        
        @Override public void addCanvasMetadata(JSONObject canvas, File f) {
                File mets = new File(f.getParentFile(), "mets.xml");
                try {
                        Document d = XMLUtil.db_ns.parse(mets);
                        setXPathValue(canvas, "label", d, "//dim:field[@element='title']");
                        addMetadata(canvas, METADATA, "Title", getXPathValue(d, "//dim:field[@element='title']", ""));
                        String dateCreated = getXPathValue(d, "//dim:field[@element='date'][@qualifier='created']","");
                        addMetadata(canvas, METADATA, "Date Created", dateCreated);
                        String dateKey = dateCreated + " " + canvas.getString("@id");
                        datecanvases.put(dateKey, canvas);
                        JSONObject dateRange = getDateRange(dateCreated);
                        if (dateRange != null) {
                                addArray(dateRange, CANVASES).put(canvas.get("@id"));
                        }

                        addMetadata(canvas, METADATA, "Creator", 
                                        getXPathValue(d, "//dim:field[@element='creator']",""));
                        addMetadata(canvas, METADATA, "Description", 
                                        getXPathValue(d, "//dim:field[@element='description'][not(@qualifier)]",""));
                        
                        StringBuilder sbSubjects = new StringBuilder();
                        try {
                                NodeList nl = (NodeList)xp.evaluate("//dim:field[@element='subject'][@qualifier='other']", d, XPathConstants.NODESET);
                                for(int i=0; i<nl.getLength(); i++) {
                                        Element selem = (Element)nl.item(i);
                                        String subj = selem.getTextContent();
                                        if (sbSubjects.length() > 0) {
                                                sbSubjects.append("; ");
                                        }
                                        //String ref = "https://repository-dev.library.georgetown.edu/handle/10822/549423#?cv=12";
                                        //sbSubjects.append("<a href='"+ref+"'>"+subj+"</a>");
                                        sbSubjects.append(subj);
                                        JSONObject subrange = subjranges.get(subj);
                                        if (subrange == null) {
                                                String subjid = subj.replaceAll(" ", "");
                                                subrange = makeRangeObject(subj, subjid, "Subject");
                                                subjranges.put(subj, subrange);
                                        }
                                        JSONObject ir = ranges.get(f.getParentFile());
                                        if (ir != null) {
                                                addArray(subrange, RANGES).put(ir.get("@id"));
                                        }
                                }
                        } catch (XPathExpressionException e) {
                        }
                        addMetadata(canvas, METADATA, "Subject(s)", sbSubjects.toString());
                        addMetadata(canvas, METADATA, "Rights", 
                                        getXPathValue(d, "//dim:field[@element='rights']",""));
                        String permalink = getXPathValue(d, "//dim:field[@element='identifier'][@qualifier='uri']",""); 
                        addMetadata(canvas, METADATA, "Permanent URL",
                                        "<a href='" + permalink + "'>" + permalink + "</a>");
                } catch (JSONException e) {
                       e.printStackTrace();
                } catch (SAXException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        public JSONObject makeRange(File dir, String label, String id, boolean isTop) {
                if (ranges.containsKey(dir)) {
                        return ranges.get(dir);
                }
                File mets = new File(dir, "mets.xml");
                if (!mets.exists()) {
                        return makeRangeObject(label, id, "Title");
                }
                try {
                        Document d = XMLUtil.db_ns.parse(mets);
                        String title = getXPathValue(d, "//dim:field[@element='title']","");
                        JSONObject obj = makeRangeObject(title, id, "Title");
                        addDirLink(dir, RANGES, id);
                        ranges.put(dir, obj);
                        //addArray(allphoto, RANGES).put(id);
                        return obj;
                } catch (JSONException e) {
                       e.printStackTrace();
                } catch (SAXException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
                
                return makeRangeObject(label, id, "Title");
        }       

        @Override public void linkCanvas(File f, String canvasid) {
                addDirLink(f, CANVASES, canvasid);
        }

        @Override public void refine() {
                for(JSONObject canvas: datecanvases.values()) {
                        addArray(seq, CANVASES).put(canvas);
                }
                for(JSONObject range: dateranges.values()) {
                        String name = String.format("%s (%d)", range.getString("label"), range.getJSONArray("canvases").length());
                        range.put("label", name);
                        addArray(alldates, RANGES).put(range);
                }
                for(JSONObject subrange: subjranges.values()) {
                        addArray(allsubjects, RANGES).put(subrange.get("@id"));
                }
        }

        @Override public void addCanvasToManifest(JSONObject canvas) {
                //no op - will add based on dates
        }

public class IIIFManifestDC extends IIIFManifest {
        
        public IIIFManifestDC(File root, String iiifRootPath, File manifestFile) {
                super(root, iiifRootPath, manifestFile);
                set2Page();
        }       
        
        @Override public ManifestDimensions getDimensions() {
                return ManifestDimensions.PORTRAIT;
        }

        public void setDC(Document d) {
                if (d == null) {
                        return;
                }
                setXPathValue(jsonObject, "description", d, "/dublin_core/dcvalue[@element='title']");
                setXPathValue(jsonObject, "label", d, "/dublin_core/dcvalue[@element='description'][@qualifier='none']");
                setXPathValue(jsonObject, "attribution", d, "/dublin_core/dcvalue[@element='rights']");

                this.addMetadata(jsonObject, METADATA, "Collection", 
                        "<a href='https://repository.library.georgetown.edu/handle/10822/552780'>DigitalGeorgetown - Hoya Collection</a>");
                this.addMetadata(jsonObject, METADATA, "Creator", 
                        getXPathValue(d, "/dublin_core/dcvalue[@element='creator']",""));
                this.addMetadata(jsonObject, METADATA, "Publisher", 
                        getXPathValue(d, "/dublin_core/dcvalue[@element='publisher']",""));
                this.addMetadata(jsonObject, METADATA, "Date Created", 
                        getXPathValue(d, "/dublin_core/dcvalue[@element='date'][@qualifier='created']",""));
                try {
                        NodeList nl = (NodeList)xp.evaluate("/dublin_core/dcvalue[@element='subject']", d, XPathConstants.NODESET);
                        for(int i=0; i<nl.getLength(); i++) {
                                Element s = (Element)nl.item(i);
                                this.addMetadata(jsonObject, METADATA, "subject", s.getTextContent());
                        }
                } catch (XPathExpressionException e) {
                }

        }
        
        private static Pattern pItem = Pattern.compile("^.*_(\\d+)\\.tif$");
        public String translateItemLabel(String label) {
                Matcher m = pItem.matcher(label);
                if (!m.matches()) {
                        return label;
                }
                try {
                        return String.format("p. %s", Integer.parseInt(m.group(1)));
                } catch (NumberFormatException e) {
                        return label;
                }
        }
}

public class IIIFManifestEAD extends IIIFManifest {
        HashMap<FolderIndex,JSONObject> folderRanges = new HashMap<>();
        class FolderIndex {
                String box = "";
                String folderStart = "";
                String folderEnd = "";
                Pattern p = Pattern.compile("^(.*)-(.*)$");
                Pattern pdir = Pattern.compile("^b(.*)_f(.*)$");
                FolderIndex(String box, String folder) {
                        this.box = normalize(box);
                        Matcher m = p.matcher(folder);
                        if (m.matches()) {
                                folderStart = normalize(m.group(1));
                                folderEnd = normalize(m.group(2));
                        } else {
                                folderStart = normalize(folder);
                                folderEnd = normalize(folder);                                
                        }
                }
                
                public String normalize(String s) {
                        try {
                                int i = Integer.parseInt(s);
                                return String.format("%06d", i);
                        } catch(NumberFormatException e) {
                                return s;
                        }
                }
                public boolean inRange(String dirName) {
                        Matcher m = pdir.matcher(dirName);
                        if (m.matches()) {
                                if (normalize(m.group(1)).equals(box)) {
                                        String f = normalize(m.group(2));
                                        if (f.compareTo(folderStart) >= 0 && f.compareTo(folderEnd) <= 0) {
                                                return true;
                                        }
                                }
                        }
                        return false;
                }
        }
        
        public IIIFManifestEAD(File root, String iiifRootPath, File manifestFile) {
                super(root, iiifRootPath, manifestFile);
                SimpleNamespaceContext nsContext = new XMLUtil().new SimpleNamespaceContext();
                nsContext.add("ead", "urn:isbn:1-931666-22-9");
                nsContext.add("ns2", "http://www.w3.org/1999/xlink");
                xp.setNamespaceContext(nsContext);
                jsonObject.put("attribution", "Georgetown Law Library");
        }       
        
        @Override public ManifestDimensions getDimensions() {
                return ManifestDimensions.PORTRAIT;
        }
        public void setEAD(Document d) {
                if (d == null) {
                        return;
                }
                setXPathValue(jsonObject, "label", d, "concat(/ead:ead/ead:archdesc/ead:did/ead:unitid,': ',/ead:ead/ead:archdesc/ead:did/ead:unittitle)");
                makeEADRanges(top, d, "//ead:c01");
                JSONObject fs = makeRange(root, "All Boxes and Folders","file-system", true);
                addArray(top, RANGES).put(fs.getString("@id"));
        }
        
        public void makeEADRanges(JSONObject parent, Node n, String xq) {
                try {
                        NodeList nl = (NodeList)xp.evaluate(xq, n, XPathConstants.NODESET);
                        for(int i=0; i<nl.getLength(); i++) {
                                Element c0 = (Element)nl.item(i);
                                JSONObject range = makeRangeObject(getXPathValue(c0, "ead:did/ead:unittitle","label"), c0.getAttribute("id"), "Container Title");
                                addArray(parent, RANGES).put(range.getString("@id"));
                                addMetadata(range, METADATA, "level", c0.getAttribute("level"));
                                makeEADRanges(range, c0, "ead:c02");
                                String box = getXPathValue(c0, "ead:did/ead:container[@type='Box']","");
                                String folder = getXPathValue(c0, "ead:did/ead:container[@type='Folder']","");
                                String boxlab = "";
                                if (!box.isEmpty()) {
                                        boxlab = String.format("Box %s; ", box);
                                        if (!folder.isEmpty()) {
                                                boxlab += String.format("Folder %s", folder);
                                        }
                                }
                                if (!boxlab.isEmpty()) {
                                        addMetadata(range, METADATA, "Container", String.format("Box %s; Folder %s", box, folder));
                                        FolderIndex folderIndex = new FolderIndex(box, folder);
                                        folderRanges.put(folderIndex, range);
                                }
                        }
                } catch (XPathExpressionException e) {
                        e.printStackTrace();
                }
                
        }
        

        public String translateLabel(String label) {
                return label
                        .replaceAll("box_","Box ")
                        .replaceAll("^b", "Box ")
                        .replaceAll("_f", " Folder ")
                        ;
        }
        
        public JSONObject makeRange(File dir, String label, String id, boolean isTop) {
                if (ranges.containsKey(dir)) {
                        return ranges.get(dir);
                }
                if (!isTop) {
                        File pfile = dir.getParentFile();
                        if (!ranges.containsKey(pfile)) {
                                //root path will always be found
                                makeRange(pfile, pfile.getName(), pfile.getName(), false);
                        }                        
                }
                
                JSONObject obj = makeRangeObject(label, id, "Directory");
                addDirLink(dir, RANGES, id);
                for(FolderIndex folderIndex: folderRanges.keySet()) {
                        if (folderIndex.inRange(label)) {
                                JSONObject range = folderRanges.get(folderIndex);
                                addArray(range, RANGES).put(id);
                        }
                }
                ranges.put(dir, obj);
                return obj;
        }       

        public String translateItemLabel(String label) {
                return label
                        .replaceAll(".*_item_", "");
        }

        @Override public void linkCanvas(File f, String canvasid) {
                addDirLink(f, CANVASES, canvasid);
        }

 }

 */
