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
