package perros;

import java.io.StringWriter;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

class StatsHandler {
    @Get
    public void handle(Request request) throws Exception {
        int pages = Integer.parseInt(request.queryParams().get("pages"));
        String stats = generateStats(pages);
        request.finish(200, stats);
    }

    private String generateStats(int pages) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("stats");
        doc.appendChild(root);

        for (int i = 0; i < pages; ++i) {
            Element stat = doc.createElement("stat" + Integer.toString(i));
            stat.appendChild(doc.createTextNode("1"));
            root.appendChild(stat);
        }

        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StringWriter writer = new StringWriter();
        tr.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.getBuffer().toString();
    }
}
