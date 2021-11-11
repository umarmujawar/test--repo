pipeline {
    agent any
    stages {
      stage('ProtectedBranchScan') {
        steps {
            sh (
              label: "protected branch cloning",
              script: """
                export date=`date +%Y-%m-%dT%H:%M:%SZ`
                user=$(echo "ghprbAuthorRepoGitUrl"|awk -F '/' '{print$4}')
                basename=$(basename ghprbAuthorRepoGitUrl)
                echo $basename
                repo=${basename%.*}
                echo $repo
                echo "Trying to fix the directory issue"
                echo "--------------"
                echo "ghprbAuthorRepoGitUrl   git url"
                echo "ghprbSourceBranch feature branch"
                echo "ghprbTargetBranch protected branch"

                ##### Creating folder to store delta file of .secrets.baseline file
                mkdir -p $PWD/$repo/delta/
                mkdir -p $PWD/$repo/$ghprbTargetBranch
                
                ####### Cloning main branch code (Protected branch)
                cd $PWD/$repo/$ghprbTargetBranch
                git clone ghprbAuthorRepoGitUrl -b ghprbTargetBranch .

                """
            )                                    
          if (fileExists(".secrets.baseline")) {  
            sh (
                label: "Scaning protected branch...",
                script: """
                  export date=`date +%Y-%m-%dT%H:%M:%SZ`
                  user=$(echo "ghprbAuthorRepoGitUrl"|awk -F '/' '{print$4}')
                  basename=$(basename ghprbAuthorRepoGitUrl)
                  echo $basename
                  repo=${basename%.*}
                  echo $repo
                  sic scan --baseline .secrets.baseline --allowlisted
                  cd ../..
                  jq -S  . $PWD/$repo/ghprbTargetBranch/.secrets.baseline > $PWD/$repo/delta/$repo-ghprbTargetBranch-baseline
                  """
            )
          }
          else {
            sh (
                label: "Scaning protected branch...",
                script: """
                  export date=`date +%Y-%m-%dT%H:%M:%SZ`
                  user=$(echo "ghprbAuthorRepoGitUrl"|awk -F '/' '{print$4}')
                  basename=$(basename ghprbAuthorRepoGitUrl)
                  echo $basename
                  repo=${basename%.*}
                  echo $repo
                  echo "Secrtes file not found so getting it from S3"
                  aws s3 cp s3://sic-tool/master-secret/.secrets.baseline .
                  sic scan --baseline .secrets.baseline --allowlisted
                  cd ../..
                  jq -S  . $PWD/$repo/ghprbTargetBranch/.secrets.baseline > $PWD/$repo/delta/$repo-ghprbTargetBranch-baseline
                  """
            )
          }  
        }
      }



      stage('FeatureBranchScan') {
        steps {
            sh (
                label: "detecting secret.baseline file...",
                script: """
                  export date=`date +%Y-%m-%dT%H:%M:%SZ`
                  user=$(echo "ghprbAuthorRepoGitUrl"|awk -F '/' '{print$4}')
                  basename=$(basename ghprbAuthorRepoGitUrl)
                  echo $basename
                  repo=${basename%.*}
                  echo $repo
                  mkdir -p $PWD/$repo/ghprbTargetBranch
                  cd $PWD/$repo/ghprbTargetBranch
                  git clone ghprbAuthorRepoGitUrl -b ghprbTargetBranch .
                  """
            )
          if (fileExists(".secrets.baseline")) {  
            sh(
                label: "Changing to JSON format...",
                script: """
                  export date=`date +%Y-%m-%dT%H:%M:%SZ`
                  user=$(echo "ghprbAuthorRepoGitUrl"|awk -F '/' '{print$4}')
                  basename=$(basename ghprbAuthorRepoGitUrl)
                  echo $basename
                  repo=${basename%.*}
                  echo $repo
                  cd ../..
                  jq -S  . $PWD/$repo/ghprbTargetBranch/.secrets.baseline > $PWD/$repo/delta/$repo-ghprbTargetBranch-baseline
                  """
            )
          }
          else {
              emailext to: "${Email}",
              subject: "No Leaks found in the Repository ${Repository} in Branch ${Branch}",
              body: "Hello,\nNo leaks found in ${Repository} in Branch ${Branch} . \n\nThanks and Regards,\ncia_security@tavisca.com",
              attachLog: false

            sh (
                label: "Scaning Feature branch...",
                script: """
                  export date=`date +%Y-%m-%dT%H:%M:%SZ`
                  user=$(echo "ghprbAuthorRepoGitUrl"|awk -F '/' '{print$4}')
                  basename=$(basename ghprbAuthorRepoGitUrl)
                  echo $basename
                  repo=${basename%.*}
                  echo $repo
                  aws s3 cp s3://sic-tool/master-secret/.secrets.baseline .
                  sic scan --baseline .secrets.baseline --allowlisted
                  cd ../..
                  jq -S  . $PWD/$repo/ghprbTargetBranch/.secrets.baseline > $PWD/$repo/delta/$repo-ghprbTargetBranch-baseline
                  """
            )
          }
        }
      }
                            

      stage('Delta-Metadata') {
        steps {
            sh (
                label: "Creating Secret-Delta File and Fetching Metadata of PR...",
                script: """
                  export date=`date +%Y-%m-%dT%H:%M:%SZ`
                  user=$(echo "ghprbAuthorRepoGitUrl"|awk -F '/' '{print$4}')
                  basename=$(basename ghprbAuthorRepoGitUrl)
                  echo $basename
                  repo=${basename%.*}
                  echo $repo
                  diff -I 'generated_at'  $PWD/$repo/delta/$repo-ghprbTargetBranch-baseline $PWD/$repo/delta/$repo-ghprbSourceBranch-baseline > $PWD/$repo/delta/$repo-ghprbSourceBranch-baseline-Delta-File.json
                  if [ $? -eq 1 ]; then
                  diff --unified  $PWD/$repo/ghprbSourceBranch/.secrets.baseline $PWD/$repo/ghprbTargetBranch/.secrets.baseline |grep -v generated_at > $PWD/$repo/delta/$repo-ghprbSourceBranch-baseline-Delta-$date.json
                  aws s3 cp $PWD/$repo/delta/$repo-ghprbSourceBranch-baseline-Delta-$date.json s3://sic-tool/delta/$repo/PRMetaData/PRNumber/$ghprbPullId/$repo-ghprbSourceBranch-baseline-Delta-$date.json
                  echo "Getting PR metadata"
                  curl -H "Accept: application/vnd.github.v3+json" https://api.github.com/repos/$user/$repo/pulls/$ghprbPullId >> $PWD/$repo/delta/$repo-ghprbSourceBranch-PRMetaData-$date.json
                  aws s3 cp $PWD/$repo/delta/$repo-ghprbSourceBranch-PRMetaData-$date.json s3://sic-tool/delta/$repo/PRMetaData/PRNumber/$ghprbPullId/$repo-ghprbSourceBranch-PRMetaData-$date.json
                  else
                  echo "no diffrence found between ghprbSourceBranch and ghprbTargetBranch branch secrets files"
                  fi
                  ##### Cleaning up the workspace
                  rm -rf $PWD/$repo
                  """
            )                                
        }
      }
                            