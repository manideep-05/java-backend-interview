package with;

import java.util.List;

/**
 * Apply different operations to the same shapes by passing different
 * visitors. The shape classes never changed to gain these operations.
 */
public class Main {
    public static void main(String[] args) {
        List<Shape> shapes = List.of(new Circle(2), new Rectangle(3, 4));

        // Operation 1: total area.
        AreaVisitor areaVisitor = new AreaVisitor();
        for (Shape shape : shapes) {
            shape.accept(areaVisitor);
        }
        System.out.println("Total area = " + areaVisitor.getTotalArea());

        // Operation 2: XML export — added with zero changes to shapes.
        XmlExportVisitor xmlVisitor = new XmlExportVisitor();
        for (Shape shape : shapes) {
            shape.accept(xmlVisitor);
        }
        System.out.print(xmlVisitor.getXml());
    }
}
