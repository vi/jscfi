VERSION=%s
mkdir -p $HOME/jscfi/bin/
cd $HOME/jscfi/bin/ || exit 1;

cat > fdlinecombine.c <<\EOF
%s
EOF

gcc -02 fdlinecombine.c -o fdlinecombine&

cat > nodestatworker <<\EOF
%s
EOF

chmod +x nodestatworker

cat > diststatcollect <<\EOF
%s
EOF

chmod +x diststatcollect

printf %%s $VERSION > VERSION
