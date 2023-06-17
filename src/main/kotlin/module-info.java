module com.tfkfan.styushsoft {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires kotlin.stdlib.jdk7;
    requires java.desktop;
    requires org.apache.poi.scratchpad;
    requires org.apache.poi.ooxml;

    opens com.tfkfan.styushsoft to javafx.fxml;
    exports com.tfkfan.styushsoft;
}
