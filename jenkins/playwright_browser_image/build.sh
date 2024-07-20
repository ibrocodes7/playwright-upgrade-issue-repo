#!/bin/bash

#Options to pass example

baseImage=mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}-focal
imageName=playwright-ubuntu-v${PLAYWRIGHT_VERSION}
args=
while [ $# -gt 0 ]
do
case $1 in
    -b|--base|--base-image)
        shift
        baseImage=$1
        args="${args} --build-arg baseImage=$baseImage"
    ;;
    -i|--image|--image-tag)
        shift
        imageName=$1
    ;;
    *)
        # unknown option
        echo "Unknown option $1"
        exit 1
    ;;
esac
shift
done

baseTag=$(echo ${baseImage} | cut -f2 -d:)
basePart=$(echo ${baseImage} | cut -f1 -d:)
[ "$baseTag" == "$basePart" ] && basePart="registry.corp.example.com/scaligent/cluster_tpch"

echo "basePart=${basePart}"
echo "baseTag=${baseTag}"

imageTag=$(echo ${imageName} | cut -f2 -d:)
if [ -z "$imageName" ]; then
    imageTag="blink-${basePart##*/}-${baseTag}"
    imageName="${basePart}:${imageTag}"
elif [ "${imageName}" == "${imageTag}" ]; then
    imageTag=${imageName}
    imageName="${basePart}:${imageTag}"
fi
# Both imageTag & imageName should now be valid
echo "New image name will be ${imageName}"
echo "New image tag will be ${imageTag}"

echo docker build $args -t $imageName . \
    && echo docker push $imageName
docker build $args -t $imageName . \
    && docker push $imageName

commit=$(git rev-parse HEAD)
commit_msg=$(git log --oneline --format=%B -n 1 $commit | head -n 1)
datetime=$(TZ=":America/Los_Angeles" date "+%Y%m%d-%H:%M")
json='{"name": "'"${basePart##*/}"'", "tag": "'"${imageTag}"'", "sha": "'"${commit}"'", "commitmesg": "'"${commit_msg}"'", "date": "'"${datetime}"'"}'
echo "If this is an official image, rebuild this run with the ADD_IMAGE_TO_NEBULA parameter set to true"
echo "${imageName}"
#cat $tag >"/net/cicd/infrastructure/nebula/${tag}"
