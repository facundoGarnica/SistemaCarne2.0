module carniceria.sistemacarne {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.naming;
    requires java.sql;
    requires java.desktop;
    requires java.prefs;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;

    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;

    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens carniceria.sistemacarne to javafx.fxml;
    opens model to org.hibernate.orm.core, javafx.fxml, javafx.base;
    opens controller to javafx.fxml, javafx.base;
    opens Util to javafx.fxml, com.fasterxml.jackson.databind, javafx.base;
    opens Dto to javafx.base, javafx.fxml;

    exports carniceria.sistemacarne;
}