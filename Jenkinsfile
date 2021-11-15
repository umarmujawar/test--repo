pipeline {
    agent any

    stages {
        stage('GetPrNumber') {
            steps {
                sh(
                     label: "Getting PR number...",
                     script: """
                     echo "$ghprbPullId"
                     """
                    )
            }
        }
        
        stage('GetPRScanScript') {
            steps {
                sh(
                label: "Getting PR scaner ...",
                script: """
                aws s3 cp s3://sic-tool/deltafinder-script/secret-scan-webhook.sh .
                dos2unix secret-scan-webhook.sh
                chmod +x secret-scan-webhook.sh
                """
                )
            }
        }
        
        stage('PRScan'){
            steps {
               sh(
                label: "Getting PR scaner ...",
                script: """
                bash secret-scan-webhook.sh $ghprbAuthorRepoGitUrl $ghprbSourceBranch $ghprbTargetBranch $ghprbPullId $ghprbActualCommit $ghprbActualCommitAuthor $ghprbActualCommitAuthorEmail $ghprbPullDescription $ghprbPullLink $ghprbPullTitle $ghprbCommentBody $sha1
                """
                )
            }
        }
         
    }
    post { 
        always { 
            deleteDir() /* clean up our workspace */
        }
    }
}
