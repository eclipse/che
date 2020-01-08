#!/usr/bin/env bash

echo "Generating index.ts file..."
echo "import * as inversifyConfig from './inversify.config';
import * as commonTestMethods from './tests/CommonTestMethods';
import * as commonLSTests from './tests/CommonLSTests';
export { inversifyConfig, commonTestMethods, commonLSTests };
export * from './inversify.types';
export * from './TestConstants';
" > index.ts

listOfDirs="driver utils pageobjects"
listOfExcludes="./driver/CheReporter.ts"
for dir in $listOfDirs
do
  files=$(find ./$dir -type f)
  for file in $files
  do  
    case $file in *ts)
      for excludedFile in $listOfExcludes
      do
        if [ $excludedFile == $file ]; then
          continue
        else
          end=$((${#file}-3))
          file_without_ending=${file:0:end}
          echo "export * from '$file_without_ending';" >> index.ts
        fi
      done
      ;;
      *)
      echo "Excluding file $file - not a typescript file"
      ;;
    esac
  done
done
