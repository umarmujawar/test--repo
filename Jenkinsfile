pipeline {
    agent any

    stages {
        stage('GetPrNumber') {
            steps {
                echo "$ghprbPullId"
            }
        }
        
        stage('GetPRScanScript') {
            steps {
                aws s3 cp s3://sic-tool/deltafinder-script/secret-scan-webhook.sh .
                dos2unix secret-scan-webhook.sh
                chmod +x secret-scan-webhook.sh
            }
        }
        
        stage('PRScan'){
            steps {
               bash secret-scan-webhook.sh $ghprbAuthorRepoGitUrl $ghprbSourceBranch $ghprbTargetBranch $ghprbPullId
            }
        }
        
    }  
}
