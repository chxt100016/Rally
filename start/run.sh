
if [ -n "$1" ]; then
  cd "$1"
fi
pwd

set -eu pipefail
docker build --no-cache -f Dockerfile -t rally .
# docker build  -f Dockerfile -t fan .

docker rm -f Rally || true
docker run \
  -itd \
  -p 5006:5005 \
  -p 9481:9482 \
  -e TZ=Asia/Shanghai \
  --name=Rally \
  --restart=always \
  rally
docker image prune -f
