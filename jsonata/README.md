# Utilities and examples


1) Invoke containerized jsonata 

- see: [./jsonata-podman.sh][./jsonata-podman.sh]

a simple example to show case how you could invoke the jsonata python package inside of a container.

Note: this particular script leverages an existing IBM Concert image that already includes jsonata.


- [./src/transform.sh](./src/transform.sh) : the actual script inside the container (entrypoint) that triggers jsonata

- [./src/example-01.json](./src/example-01.json) : an example json file

- [./src/convert.jsonata](./src/convert.jsonata) : an example jsonata expression that extracts/transforms the source json and produces an alternate json output.
