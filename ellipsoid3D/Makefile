all:
	javac -cp /usr/share/java/ij.jar *.java
	mv *.class ellip/
	jar cvf Ellipsoid_3D.jar ellip/*.class plugins.config;

install:
	make all 
	cp Ellipsoid_3D.jar ~/.imagej/plugins/

clean:
	rm ellip/*.class
	rm Ellipsoid_3D.jar
