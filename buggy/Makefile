JAVAFLAGS=-XX:+PreserveFramePointer -Xcomp
JAVAEXTFLAGS=-XX:+ExtendedDTraceProbes

build:
		mkdir -p bin
		cp mysql-connector-java-5.1.40-bin.jar bin/
		javac *.java -d bin

clean:
		rm -rf bin

allocy: build
		java $(JAVAFLAGS) -cp bin Allocy

allocyext: build
		java $(JAVAFLAGS) $(JAVAEXTFLAGS) -cp bin Allocy

clientygood: build
		sudo pip install dnslib
		sudo ./proxy.py > /dev/null &
		java $(JAVAFLAGS) -cp bin Clienty good

clientybad: build
		sudo pip install dnslib
		sudo ./proxy.py > /dev/null &
		java $(JAVAFLAGS) -cp bin Clienty bad

collecty: build
		java $(JAVAFLAGS) $(JAVAEXTFLAGS) -cp bin Collecty

computey: build
		java $(JAVAFLAGS) -cp bin Computey 2 4000000 4

computeyext: build
		java $(JAVAFLAGS) $(JAVAEXTFLAGS) -cp bin Computey 2 4000000 4

databasey: build
		java $(JAVAFLAGS) -cp "bin:bin/mysql-connector-java-5.1.40-bin.jar" \
						  Databasey
databaseyext: build
		java $(JAVAFLAGS) $(JAVAEXTFLAGS) \
			-cp "bin:bin/mysql-connector-java-5.1.40-bin.jar" Databasey

servery: build
		java $(JAVAFLAGS) -cp bin Servery

writey: build
		java $(JAVAFLAGS) -cp bin Writey

all: build
