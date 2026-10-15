package with;

/**
 * A SECOND operation, added WITHOUT touching any shape class. This is
 * the payoff: new operations are new visitor classes. (Adding a new
 * SHAPE, however, would require a new visit() method on every visitor —
 * the known trade-off.)
 */
public class XmlExportVisitor implements ShapeVisitor {

    private final StringBuilder xml = new StringBuilder();

    @Override
    public void visit(Circle circle) {
        xml.append("<circle r='").append(circle.radius).append("'/>\n");
    }

    @Override
    public void visit(Rectangle rectangle) {
        xml.append("<rect w='").append(rectangle.width)
           .append("' h='").append(rectangle.height).append("'/>\n");
    }

    public String getXml() {
        return xml.toString();
    }
}
