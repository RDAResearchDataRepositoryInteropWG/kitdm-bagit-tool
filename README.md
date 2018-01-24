# KIT Data Manager BagIt Tool

The KIT Data Manager BagIt Tool allows to export and import BagIt bags following the recommendations provided by the Research Data Repository Interoperability Working Group (RDRIWG) as part of the Research Data Alliance.

It allows to export digital objects stored in KIT Data Manager-based repository platforms into bags compliant to the BagIt specification. Furthermore, DataCite metadata is added to each bag as proposed by the RDRIWG. 

The import functionality of the KIT Data Manager BagIt Tool supports the import of all bags created following the RDRIWG recommendations. If the bag was not exported from a KIT Data Manager instance, the contained DataCite metadata is used to collect minimal base metadata for the imported digital object.

## How to build

In order to build and use the KIT Data Manager BagIt Tool you'll need:

* Java SE Development Kit 8 or higher
* Apache Maven 3.3+
* KIT Data Manager 1.5+

After obtaining the sources change to the folder where the sources are located and call:

```
user@localhost:/home/user/KITDM-BagIt-Tool/base$ mvn assembly:assembly
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building KITDM-BagIt-Tool <VERSION>
[INFO] ------------------------------------------------------------------------
[...]
user@localhost:/home/user/KITDM-BagIt-Tool/base$
```

As soon as the build has finished, you'll find the command line tool at ./KITDM-BagIt-Tool-<VERSION>

## How to use

In order to make the BagIt Tool as slim as possible, it does not contain any KIT Data Manager libraries. The required dependencies are taken directly from the local KIT Data Manager installation. Therefore, the KIT Data Manager installation directory has to be assigned to the variable KIT_DATA_MANAGER_LOCATION located at the beginning of bin/BagItTool.

Afterwards, the BagIt Tool can be invoked as follows:

```
user@localhost:/home/user/KITDM-BagIt-Tool/base$ cd KITDM-BagIt-Tool-<VERSION>
user@localhost:/home/user/KITDM-BagIt-Tool/base/KITDM-BagIt-Tool-<VERSION>$ ./bin/BagItTool

Usage: RepoInteropTool [options] [command] [command options]
  Commands:
    export      Performs the export of a digital object from a local repository.
      Usage: export [options]
[...]
user@localhost:/home/user/KITDM-BagIt-Tool/base/KITDM-BagIt-Tool-<VERSION>$
```

The output of this call will provide you with usage information. Please read them carefully before starting your first export/import.

## Sample Invocations

```
./bin/BagItTool export -i 46a2bb19-8964-4d2e-83f8-b0fd514e311d -o theBag/
```

Export the digital object with identifier 46a2bb19-8964-4d2e-83f8-b0fd514e311d to a bag located at ./theBag

```
./bin/BagItTool import -i 159 -s theBag/ -u admin -g USERS
```

Import the digital object contained in ./theBag into the local repository. The resulting digital object is inserted into the investigation with identifier 159 and the ownership is set to user 'admin' and group 'USERS'. 

## Known Issues/Current Limitations

* The export only contains base metadata. It does NOT include authorization information, audit events or externally stored metadata, e.g. from MetaStore.
* Bags can only be serialized as ZIP files
* No DataCrate support, yet

## More Information

* [Research Data Repository Interoperability WG](https://rd-alliance.org/groups/research-data-repository-interoperability-wg-bof.html)
* [RDRIWG Recommendations](https://docs.google.com/document/d/1VmmhNMl4ie5zqbCKkf3NDNRHtgdb2SgYF_cEn58zt7g/edit?usp=sharing)
* [BagIt Specification](https://tools.ietf.org/html/draft-kunze-bagit-08)
* [BagIt Profiles Specification](https://github.com/ruebot/bagit-profiles)
* [DataCite 4.0 Metadata Schema](https://schema.datacite.org/meta/kernel-4.0/)

## License

The KIT Data Manager BagIt Tool is licensed under the Apache License, Version 2.0.


