DATASET=$1

res_js=$(cat $DATASET | grep "javascript," | wc -l)
echo "JS: $res_js"

res_py=$(cat $DATASET | grep "python," | wc -l)
echo "PY: $res_py"

res_jv=$(cat $DATASET | grep "java," | wc -l)
echo "JV: $res_jv"


lambdas_js=$(python -c "print((($res_js) * 416) / ($res_jv + $res_js + $res_py))")
echo "JS lambdas: $lambdas_js"
lambdas_py=$(python -c "print((($res_py) * 416) / ($res_jv + $res_js + $res_py))")
echo "PY lambdas: $lambdas_py"
lambdas_jv=$(python -c "print((($res_jv) * 416) / ($res_jv + $res_js + $res_py))")
echo "JV lambdas: $lambdas_jv"

res_js=$(cat $DATASET | grep "jshw" | wc -l)
echo "JS HelloWorld: $res_js"

res_js=$(cat $DATASET | grep "jsup" | wc -l)
echo "JS Uploader: $res_js"

res_js=$(cat $DATASET | grep "jsdh" | wc -l)
echo "JS Dynamic HTML: $res_js"

res_js=$(cat $DATASET | grep "pyhw" | wc -l)
echo "Python HelloWorld: $res_js"

res_js=$(cat $DATASET | grep "pyup" | wc -l)
echo "Python Uploader: $res_js"

res_js=$(cat $DATASET | grep "pyco" | wc -l)
echo "Python Compression: $res_js"

res_js=$(cat $DATASET | grep "jvhw" | wc -l)
echo "Java HelloWorld: $res_js"

res_js=$(cat $DATASET | grep "jvfh" | wc -l)
echo "Java FileHashing: $res_js"

res_js=$(cat $DATASET | grep "jvhr" | wc -l)
echo "Java HTTP Request: $res_js"
