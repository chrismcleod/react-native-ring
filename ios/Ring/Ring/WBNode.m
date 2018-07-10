//
//  WBNode.m
//  Ring
//
//  Created by chris on 7/5/18.
//  Copyright © 2018 Warebots, LLC. All rights reserved.
//

#import "WBNode.h"

@implementation WBNode

- (instancetype) init {
  self = [super init];
  if(self){
    self.appGroupName = @"";
    self.versionKey = @"com-warebots-reactnative-ring-version";
  }
  return self;
}

- (instancetype) initWithAppGroupName:(NSString *) appGroupName andQueue:(dispatch_queue_t)queue {
  self = [self init];
  if(self){
    self.queue = queue;
    self.appGroupName = [@"group." stringByAppendingString:appGroupName];
    self.containerUrl = [[NSFileManager defaultManager] containerURLForSecurityApplicationGroupIdentifier:self.appGroupName];
    self.containerPath = [self.containerUrl path];
    self.sharedUrl = [self.containerUrl URLByAppendingPathComponent:@"com-warebots-ring"];
    self.sharedPath = [self.sharedUrl path];
    self.dataUrl = [self.sharedUrl URLByAppendingPathComponent:@"data.json"];
    self.dataPath = [self.dataUrl path];
    self.defaults = [[NSUserDefaults alloc] initWithSuiteName:self.appGroupName];
    self.operationQueue = [[NSOperationQueue alloc] init];
  }
  return self;
}

- (int) getVersion {
  return (int)[self.defaults integerForKey:self.versionKey];
}

- (void) readData:(void (^)(BOOL success, NSDictionary *output))completionBlock {
  NSFileCoordinator *coordinator = [[NSFileCoordinator alloc] init];
  NSFileAccessIntent *readIntent = [NSFileAccessIntent readingIntentWithURL:self.dataUrl options:NSFileCoordinatorReadingWithoutChanges];
  NSArray<NSFileAccessIntent *> *intents = @[readIntent];
  [coordinator coordinateAccessWithIntents:intents queue:self.operationQueue byAccessor:^(NSError * _Nullable error) {
    if(error == nil) {
      NSString *dataPath = [[readIntent URL] path];
      NSString *dataString = [[NSString alloc] initWithData:[NSData dataWithContentsOfFile:dataPath] encoding:NSUTF8StringEncoding];
      int version = (int)[self.defaults integerForKey:self.versionKey];
      NSDictionary *output = @{@"version": @(version), @"data":dataString};
      completionBlock(true, output);
    }
  }];
}

- (void) writeData:(int)version data:(NSString *)dataString withCompletion:(void (^)(BOOL success, NSDictionary *output))completionBlock {
  if([self.defaults integerForKey:self.versionKey] != version) {
    completionBlock(false, @{});
  } else {
    NSFileCoordinator *coordinator = [[NSFileCoordinator alloc] init];
    NSFileAccessIntent *writeIntent = [NSFileAccessIntent writingIntentWithURL:self.dataUrl options:NSFileCoordinatorWritingForReplacing];
    NSArray<NSFileAccessIntent *> *intents = @[writeIntent];
    [coordinator coordinateAccessWithIntents:intents queue:self.operationQueue byAccessor:^(NSError * _Nullable error) {
      if(error == nil) {
        NSString *writePath = [[writeIntent URL] path];
        NSData *data = [dataString dataUsingEncoding:NSUTF8StringEncoding];
        [data writeToFile:writePath atomically:YES];
        [self.defaults setInteger:version+1 forKey:self.versionKey];
        NSDictionary *output = @{@"version": @(version + 1), @"data":dataString};
        completionBlock(true, output);
      }
    }];
  }
}

- (BOOL) assertFilesExist {
  BOOL isDir;
  BOOL dataDirExists = [[NSFileManager defaultManager] fileExistsAtPath:self.sharedPath isDirectory:&isDir];
  if(dataDirExists != YES) {
    [[NSFileManager defaultManager] createDirectoryAtPath:self.sharedPath withIntermediateDirectories:YES attributes:nil error:nil];
    NSString *initialData = @"{}";
    [self.defaults setInteger:-1 forKey:self.versionKey];
    [[NSFileManager defaultManager] createFileAtPath:self.dataPath contents:[initialData dataUsingEncoding:NSUTF8StringEncoding] attributes:nil];
  }
  return YES;
}

@end
